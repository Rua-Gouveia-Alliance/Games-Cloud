package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import pt.ulisboa.tecnico.cnv.loadbalancer.aws.MetricsCollector;

public class ICount extends CodeDumper {

    /**
     * Number of executed basic blocks.
     */
    private static final ThreadLocal<Long> nblocks = ThreadLocal.withInitial(() -> 0L);

    /**
     * Number of executed methods.
     */
    private static final ThreadLocal<Long> nmethods = ThreadLocal.withInitial(() -> 0L);

    /**
     * Number of executed instructions.
     */
    private static final ThreadLocal<Long> ninsts = ThreadLocal.withInitial(() -> 0L);

    public ICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void incBasicBlock(int position, int length) {
        nblocks.set(nblocks.get() + 1);
        ninsts.set(ninsts.get() + length);

        MetricsCollector.getInstance().addMetric(MetricsCollector.BASIC_BLOCKS, nblocks.get());
        MetricsCollector.getInstance().addMetric(MetricsCollector.INSTRUCTIONS, ninsts.get());
    }

    public static void incBehavior(String name) {

        nmethods.set(nmethods.get() + 1);
        MetricsCollector.getInstance().addMetric(MetricsCollector.METHODS_EXECUTED, nmethods.get());
    }

    public static void resetStatistics() {
        nblocks.set(0L);
        nmethods.set(0L);
        ninsts.set(0L);
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
            behavior.insertAfter(
                    String.format("%s.printStatistics(this.getClass().getSimpleName());",
                            ICount.class.getName()),
                    true);
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);",
                ICount.class.getName(), block.getPosition(), block.getLength()));
    }
}
