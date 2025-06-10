package pt.ulisboa.tecnico.cnv.loadbalancer;

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
        double capacity;

        public WorkerInfo(double price, double capacity) {
            this.price = price;
            this.capacity = capacity;
        }
    }

    static private HashMap<String, WorkerInfo> workers = new HashMap<>() {
        {
            // Since we're only using a single type of VM, price can be set to anything
            put("VM", new WorkerInfo(1.0, 1.0));
        }
    };

    static private HashMap<String, BigDecimal> computeConfiguration(double lambda) {
        HashMap<String, AbstractMap.SimpleEntry<Integer, Variable>> variables = new HashMap<>();
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        int i = 0;
        for (Map.Entry<String, WorkerInfo> e : workers.entrySet()) {
            String name = e.getKey();
            WorkerInfo info = e.getValue();
            AbstractMap.SimpleEntry<Integer, Variable> entry = new AbstractMap.SimpleEntry<>(i++,
                    model.newVariable(name).integer(true).lower(0).weight(info.price));
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

        // Minimize cost
        Optimisation.Result result = model.minimise();

        HashMap<String, BigDecimal> configuration = new HashMap<>();
        for (Map.Entry<String, AbstractMap.SimpleEntry<Integer, Variable>> e : variables
                .entrySet()) {
            String name = e.getKey();
            AbstractMap.SimpleEntry<Integer, Variable> entry = e.getValue();
            BigDecimal count = result.get(entry.getKey().intValue());
            configuration.put(name, count);
        }
        System.out.println("Total cost: " + result.getValue());
        return configuration;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java pt.ulisboa.tecnico.cnv.autoscaler.Autoscaler <lambda>");
            return;
        }

        double lambda = Double.parseDouble(args[0]);
        HashMap<String, BigDecimal> config = computeConfiguration(lambda);
        for (Map.Entry<String, BigDecimal> e : config.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue());
        }
    }
}
