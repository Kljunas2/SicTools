ints	START 0


	ORG 0x130
	. first byte will be filled by the interrupt's value
	. second nibble of the second byte is interrupt mask (0 = disable all interrupts)
	. the third byte goes to supervisor mode
prog_int	WORD 0x000001
prog_haddr	RESW 1
	RESW 10 . 9 registers, one of them 2 WORDS long

prog_handler
	TD #1
	LPS #prog_int

rand	WORD 1


init_regs RESW 2
	WORD 0x000400 . enable PROG, user mode
init_addr	RESW 1
	RESW 8 . init all registers to 0


sw	RESW 1

	. switch to user mode, enable interrupts
entry	LDA #prog_handler
	STA prog_haddr
	LDA #program
	STA init_addr
	STI #40
	LPS #init_regs

program	TD #1
	J program

	END entry
