package ru.dmkuranov.aspects_util.aspectj.exectime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CallStackFrame {
    private final String name;
    private final CallStackFrame parentFrame;
    private final List<CallStackFrame> childFrames = new ArrayList<CallStackFrame>();
    private final long startNs = System.nanoTime();
    private long finishNs;

    public CallStackFrame(String name, CallStackFrame parentFrame) {
        this.name = name;
        this.parentFrame = parentFrame;
    }

    public void addChildFrame(CallStackFrame childFrame) {
        childFrames.add(childFrame);
    }

    public List<CallStackFrame> getChildFrames() {
        return Collections.unmodifiableList(childFrames);
    }

    public CallStackFrame getParentFrame() {
        return parentFrame;
    }
    public void finish() {
        finishNs = System.nanoTime();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder outp = new StringBuilder();
        appendTo(outp, 0);
        return outp.toString();
    }
    protected void appendTo(StringBuilder sb, int level) {
        for(int i=0;i<level;i++) {
            sb.append("\t");
        }
        sb.append(String.format("%.4f %s\n", ((finishNs-startNs)/1000000000d), name));
        for(CallStackFrame childFrame: childFrames) {
            childFrame.appendTo(sb, level+1);
        }
    }
}
