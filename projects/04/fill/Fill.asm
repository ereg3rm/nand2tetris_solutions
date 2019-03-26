// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.

(LOOP)
@24576
D=M
@BLACKEN
D;JGT

//(CLEAR)
@col
M=0
@DRAW
0;JMP

(BLACKEN)
@col
M=-1


(DRAW)
@16384
D=A
@base
M=D
@i
M=0
(DRAW_LOOP)
@i
D=M
@8192
D=D-A
@LOOP
D;JGE

@col
D=M
@base
A=M
M=D

@i
M=M+1
@base
M=M+1


@DRAW_LOOP
0;JMP
