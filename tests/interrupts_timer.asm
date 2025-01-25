ints	START 0


	ORG 0x160
	. first byte will be filled by the interrupt's value
	. second nibble of the second byte is interrupt mask (0 = disable all interrupts)
	. the third byte goes to supervisor mode
svc_int	WORD 0x000001
svc_haddr	RESW 1
	RESW 10 . 9 registers, one of them 2 WORDS long

svc_handler
	LDCH #0x0a
	WD #1
	LDCH #0x54
	WD #1
	LDCH #0x49
	WD #1
	LDCH #0x4d
	WD #1
	LDCH #0x0a
	WD #1
	CLEAR A
	RD #3
	STA rand
	STI rand
	//SVC 13

	LPS #svc_int

rand	WORD 1


init_regs RESW 2
	WORD 0x000200 . enable SVC, user mode
init_addr	RESW 1
	RESW 8 . init all registers to 0


sw	RESW 1

	. switch to user mode, enable interrupts
entry	LDA #svc_handler
	STA svc_haddr
	LDA #program
	STA init_addr
	STI #40
	LPS #init_regs

program	LDCH #0x41
	WD #1
halt	J program

	END entry
