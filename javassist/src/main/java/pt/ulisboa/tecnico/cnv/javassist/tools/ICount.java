package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import pt.ulisboa.tecnico.cnv.loadbalancer.aws.DynamoUploader;
import pt.ulisboa.tecnico.cnv.loadbalancer.aws.Metric;
import pt.ulisboa.tecnico.cnv.loadbalancer.aws.MetricsCollector;

public class ICount extends CodeDumper {

    /**
     * Number of executed basic blocks.
     */
    private static final ThreadLocal<Integer> nblocks = ThreadLocal.withInitial(() -> 0);

    /**
     * Number of executed methods.
     */
    private static final ThreadLocal<Integer> nmethods = ThreadLocal.withInitial(() -> 0);

    /**
     * Number of executed instructions.
     */
    private static final ThreadLocal<Integer> ninsts = ThreadLocal.withInitial(() -> 0);

    public ICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void incBasicBlock(int position, int length) {
        nblocks.set(nblocks.get() + 1);
        ninsts.set(ninsts.get() + length);
    }

    public static void incBehavior(String name) {
        nmethods.set(nmethods.get() + 1);
    }

    public static void resetStatistics() {
        nblocks.set(0);
        nmethods.set(0);
        ninsts.set(0);
    }

    public static void printStatistics(String className) {
        try {
            FileWriter fwriter = new FileWriter(String.format("output/%s.txt", className), true);
            fwriter.write(String.format("M: %s; BB: %s; I: %s\n", nmethods.get(), nblocks.get(),
                    ninsts.get()));
            fwriter.close();
            System.out.println(String.format("[%s] Successfully logged metrics for %s",
                    ICount.class.getSimpleName(), className));
        } catch (IOException e) {
            System.out.println(String.format("[%s] Failed logging metrics for %s",
                    ICount.class.getSimpleName(), className));
            e.printStackTrace();
        }
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", ICount.class.getName(),
                behavior.getLongName()));

        String behaviorName = behavior.getName();
        if (behaviorName.equals("handleWorkload")) {
            behavior.insertBefore(String.format("%s.resetStatistics();", ICount.class.getName()));

            behavior.insertAfter("{ " + "java.util.Map params = new java.util.HashMap();"
                    + "for (int i = 0; i < $args.length; i++) { "
                    + "if ($args[i] instanceof Integer) { "
                    + "params.put(\"arg\" + i, (Integer) $args[i]); } }"
                    + String.format(
                            "%s.getInstance().addMetric(new %s(this.getClass().getSimpleName(), %s.now(), params, %dL, %dL, %dL));",
                            MetricsCollector.class.getName(), Metric.class.getName(),
                            LocalDateTime.class.getName(), nmethods.get(), nblocks.get(),
                            ninsts.get())
                    + String.format("%s.accumulateItem();", DynamoUploader.class.getName()) + " }",
                    true);
            System.out.print("[ICount] Successfuly instrumented method");
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);",
                ICount.class.getName(), block.getPosition(), block.getLength()));
    }
}
