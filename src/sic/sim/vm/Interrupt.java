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

    public enum ProgICODE {
        ILLEGAL_INSTRUCTION(0x00),
        PRIVILEGED_INSTRUCTION(0x01),
        ADDR_OUT_OF_RANGE(0x02),
        MEM_PROTECT(0x03),
        OVERFLOW(0x04),
        PAGE_FAULT(0x10),
        SEG_FAULT(0x11),
        SEG_PROTECTION_VIOLATION(0x12),
        SEG_LEN_EXCEEDED(0x13);

        public final int value;

        ProgICODE(int value) {
            this.value = value;
        }
    }

    public IClass CLASS;
    private int ICODE;

    public Interrupt(IClass iclass, int icode) {
        CLASS = iclass;
        ICODE = icode;
    }

    public Interrupt(IClass iclass, ProgICODE icode) {
        CLASS = iclass;
        ICODE = icode.value;
    }

    public static int getWorkArea(IClass CLASS) {
        switch (CLASS) {
            case SVC:
                return 0x100;
            case PROGRAM:
                return 0x130;
            case TIMER:
                return 0x160;
            case IO:
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
        registers.setSW(memory.getWord(addr));
        registers.setPC(memory.getWord(addr+3));
        registers.setICODE(ICODE);
    }
}
