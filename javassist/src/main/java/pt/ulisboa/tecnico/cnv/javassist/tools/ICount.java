package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtBehavior;

public class ICount extends CodeDumper {

    /**
     * Number of executed basic blocks.
     */
    private static long nblocks = 0;

    /**
     * Number of executed methods.
     */
    private static long nmethods = 0;

    /**
     * Number of executed instructions.
     */
    private static long ninsts = 0;

    public ICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void incBasicBlock(int position, int length) {
        nblocks++;
        ninsts += length;
    }

    public static void incBehavior(String name) {
        nmethods++;
    }

    public static void resetStatistics() {
        nmethods = nblocks = ninsts = 0;
    }

    public static void printStatistics(String className) {
        try {
            FileWriter fwriter = new FileWriter(String.format("output/%s.txt", className), true);
            fwriter.write(String.format("M: %s; BB: %s; I: %s\n", nmethods, nblocks, ninsts));
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
        if (behaviorName.equals("handle") || behaviorName.equals("handleRequest")) {
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
