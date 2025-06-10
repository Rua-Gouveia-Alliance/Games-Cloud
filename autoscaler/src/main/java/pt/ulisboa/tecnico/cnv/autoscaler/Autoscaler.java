package pt.ulisboa.tecnico.cnv.autoscaler;

import java.math.BigDecimal;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Variable;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.Optimisation;

public class Autoscaler {

    static private class WorkerInfo {
        double price;
        double latency;
        double capacity;

        public WorkerInfo(double price, double latency, double capacity) {
            this.price = price;
            this.latency = latency;
            this.capacity = capacity;
        }
    }

    static private HashMap<String, WorkerInfo> workers = new HashMap<>() {
        {
            put("VM", new WorkerInfo(5.0, 20.0, 1.0));
            put("FaaS", new WorkerInfo(100.0, 10.0, 1.0));
        }
    };

    static private HashMap<String, BigDecimal> computeConfiguration(Double lambda, Double B) {
        HashMap<String, AbstractMap.SimpleEntry<Integer, Variable>> variables = new HashMap<>();
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        int i = 0;
        for (Map.Entry<String, WorkerInfo> e : workers.entrySet()) {
            String name = e.getKey();
            WorkerInfo info = e.getValue();
            AbstractMap.SimpleEntry<Integer, Variable> entry =
                    new AbstractMap.SimpleEntry<>(i++, model.newVariable(name).integer(true)
                            .lower(0).weight(info.price + B * info.latency));
            variables.put(name, entry);
        }

        // Constraints
        Expression capacity = model.addExpression("Capacity");
        for (Map.Entry<String, WorkerInfo> e : workers.entrySet()) {
            String name = e.getKey();
            WorkerInfo info = e.getValue();
            capacity.set(variables.get(name).getValue(), info.capacity);
        }
        capacity.lower(lambda);

        // Minimize cost + B * latency
        Optimisation.Result result = model.minimise();

        HashMap<String, BigDecimal> configuration = new HashMap<>();
        for (Map.Entry<String, AbstractMap.SimpleEntry<Integer, Variable>> e : variables
                .entrySet()) {
            String name = e.getKey();
            AbstractMap.SimpleEntry<Integer, Variable> entry = e.getValue();
            BigDecimal count = result.get(entry.getKey().intValue());
            configuration.put(name, count);
        }
        System.out.println("Total cost (price + latency): " + result.getValue());
        return configuration;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println(
                    "Usage: java pt.ulisboa.tecnico.cnv.autoscaler.Autoscaler <lambda> <B>");
            return;
        }

        Double lambda = Double.parseDouble(args[0]);
        Double B = Double.parseDouble(args[1]);

        HashMap<String, BigDecimal> config = computeConfiguration(lambda, B);
        for (Map.Entry<String, BigDecimal> e : config.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue());
        }
    }
}
//
