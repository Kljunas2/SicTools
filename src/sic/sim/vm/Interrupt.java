package sic.sim.vm;

import sic.sim.breakpoints.ReadDataBreakpointException;
import sic.sim.breakpoints.WriteDataBreakpointException;

public class Interrupt {
    public enum IClass {
        SVC(8),
        PROGRAM(4),
        TIMER(2),
        IO(1);

        public final int value;

        IClass(int value) {
            this.value = value;
        }
    }

    public IClass CLASS;
    private int ICODE;

    public Interrupt(IClass iclass, int icode) {
        CLASS = iclass;
        ICODE = icode;
        //System.out.printf("created interrupt %s, icode. 0x%x%n", CLASS.name(), ICODE);
    }

    public static int getWorkArea(IClass CLASS) {
        switch (CLASS) {
            case IClass.SVC:
                return 0x100;
            case IClass.PROGRAM:
                return 0x130;
            case IClass.TIMER:
                return 0x160;
            case IClass.IO:
                return 0x190;
        }
        return -1;
    }

    private void saveRegisters(Registers registers, Memory memory) throws WriteDataBreakpointException {
        int addr = Interrupt.getWorkArea(CLASS);
        memory.setWord(addr+6, registers.getSW());
        memory.setWord(addr+9, registers.getPC());
        memory.setWord(addr+12, registers.getA());
        memory.setWord(addr+15, registers.getX());
        memory.setWord(addr+18, registers.getL());
        memory.setWord(addr+21, registers.getB());
        memory.setWord(addr+24, registers.getS());
        memory.setWord(addr+27, registers.getT());
        memory.setFloat(addr+30, registers.getF());
    }

    private void restoreRegisters(Registers registers, Memory memory) throws ReadDataBreakpointException {
        int addr = Interrupt.getWorkArea(CLASS);
    }

    public void trigger(Registers registers, Memory memory)  throws ReadDataBreakpointException, WriteDataBreakpointException {
        saveRegisters(registers, memory);
        int addr = Interrupt.getWorkArea(CLASS);
        //System.out.printf("triggering interrupt %s, icode: 0x%x, jumping to: 0x%6x%n", CLASS.name(), ICODE, addr);
        registers.setSW(memory.getWord(addr));
        registers.setPC(memory.getWord(addr+3));
    }

    /*
    public enum ProgramType {
        ILLEG_INSTR,
        PRIVILEGED_INSTR,
        ADDR_OUT_OF_RANGE,
        MEM_PROTECT,
        OVERFLOW,
        PAGE_FAULT,
        SEG_FAULT,
        SEG_PROTECT_VIOLATION,
        SEG_LEN_EXCEEDED,
    }
    */
}
