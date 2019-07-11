/**
* @author eregerm
*/

import java.io.*;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Scanner;

/**
* assembler
*/
public class Assembler {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: Assembler asmfile");
			return;
		}
		doAssembly(args[0]);
	}
	
	/**
	* assemble asm into hack
	*/
	private static void doAssembly(String file) {
		new ParserAndCode(file);
	}
}

enum CommandType {
	A_COMMAND,A_COMMAND_L,C_COMMAND_0,C_COMMAND_1,C_COMMAND_2,C_COMMAND_3,L_COMMAND,NO_COMMAND
}

class ParserAndCode {
	private final String USER_DEFINED = "^[a-zA-Z_\\.$:][a-zA-Z0-9_\\.$:]*$";
	
	private String filePath;
	private String fileName;
	private HashMap<String, String> compMap = new HashMap<String, String>();
	private HashMap<String, String> destMap = new HashMap<String, String>();
	private HashMap<String, String> jumpMap = new HashMap<String, String>();
	private HashMap<String, Integer> symbolTable = new HashMap<String, Integer>();
	private StringBuilder oriFile = new StringBuilder("");
	private StringBuilder hackFile = new StringBuilder("");
	private int variableCount = 16;
	private int labelCount = 0;
	
	public ParserAndCode(String file) {
		File f = new File(file);
		int dotPos = f.getName().indexOf(".");
		if(!f.getName().substring(dotPos + 1).equals("asm")) {
			System.out.println("Not asm file.");
			System.exit(1);
		}
		fileName = f.getName().substring(0, dotPos);
		filePath = f.getPath().substring(0, f.getPath().lastIndexOf(fileName+".asm"));
		//System.out.println(filePath);
		try {
			InputStream is = new FileInputStream(file);
			int size = is.available();
			for (int i = 0; i < size; i++) {
				oriFile.append((char)is.read());
			}
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		init();
		onePass();
		doParseAndCode();
		generateHack();
	}
	
	/**
	* add fields and predefined symbols
	*/
	private void init() {
		compMap.put("0", "0101010");
		compMap.put("1", "0111111");
		compMap.put("-1", "0111010");
		compMap.put("D", "0001100");
		compMap.put("A", "0110000");compMap.put("M", "1110000");
		compMap.put("!D", "0001101");
		compMap.put("!A", "0110001");compMap.put("!M", "1110001");
		compMap.put("-D", "0001111");
		compMap.put("-A", "0110011");compMap.put("-M", "1110011");
		compMap.put("D+1", "0011111");
		compMap.put("A+1", "0110111");compMap.put("M+1", "1110111");
		compMap.put("D-1", "0001110");
		compMap.put("A-1", "0110010");compMap.put("M-1", "1110010");
		compMap.put("D+A", "0000010");compMap.put("D+M", "1000010");
		compMap.put("D-A", "0010011");compMap.put("D-M", "1010011");
		compMap.put("A-D", "0000111");compMap.put("M-D", "1000111");
		compMap.put("D&A", "0000000");compMap.put("D&M", "1000000");
		compMap.put("D|A", "0010101");compMap.put("D|M", "1010101");
		destMap.put("", "000");
		destMap.put("M", "001");
		destMap.put("D", "010");
		destMap.put("MD", "011");
		destMap.put("A", "100");
		destMap.put("AM", "101");
		destMap.put("AD", "110");
		destMap.put("AMD", "111");
		jumpMap.put("", "000");
		jumpMap.put("JGT", "001");
		jumpMap.put("JEQ", "010");
		jumpMap.put("JGE", "011");
		jumpMap.put("JLT", "100");
		jumpMap.put("JNE", "101");
		jumpMap.put("JLE", "110");
		jumpMap.put("JMP", "111");
		for (int i = 0; i < 16; i++) symbolTable.put("R"+i, i);
		symbolTable.put("SP", 0);
		symbolTable.put("LCL", 1);
		symbolTable.put("ARG", 2);
		symbolTable.put("THIS", 3);
		symbolTable.put("THAT", 4);
		symbolTable.put("SCREEN", 16384);
		symbolTable.put("KBD", 24576);
	}
	
	/**
	* add label symbols
	*/
	private void onePass() {
		int lineNum = 0;
		labelCount = 0;
		Scanner sc = new Scanner(oriFile.toString());
		while(sc.hasNextLine()) {
			lineNum++;
			String nowLine = sc.nextLine();
			int comPos = nowLine.indexOf("//");
			if (comPos != -1) nowLine = nowLine.substring(0, comPos);
			nowLine = nowLine.trim();
			if (nowLine.equals("")) continue;
			CommandType nowLineType = isCommandType(nowLine);
			if (nowLineType == CommandType.L_COMMAND) {
				int len = nowLine.length();
				String symbol = nowLine.substring(1, len - 1);
				if (!symbolTable.containsKey(symbol))symbolTable.put(symbol, labelCount);
				else invalidSyntax(lineNum);//error
			} else if (nowLineType != CommandType.NO_COMMAND)labelCount++;
		}
		sc.close();
	}
	
	/**
	* add variable symbols and generate binary codes
	*/
	private void doParseAndCode() {
		int lineNum = 0;
		Scanner sc = new Scanner(oriFile.toString());
		while(sc.hasNextLine()) {
			lineNum++;
			String nowLine = sc.nextLine();
			int comPos = nowLine.indexOf("//");
			if (comPos != -1) nowLine = nowLine.substring(0, comPos);
			nowLine = nowLine.trim();
			if (nowLine.equals("")) continue;
			//System.out.println(nowLine);
			CommandType nowLineType = isCommandType(nowLine);
			if (nowLineType == CommandType.A_COMMAND) {
				String symbol = nowLine.substring(1);
				if (!symbolTable.containsKey(symbol))symbolTable.put(symbol, variableCount++);
				String bCode = padZero(Integer.toBinaryString(symbolTable.get(symbol)), 16);
				hackFile.append(bCode + '\n');
			} else if (nowLineType == CommandType.A_COMMAND_L) {
				int im = Integer.valueOf(nowLine.substring(1));
				String bCode = padZero(Integer.toBinaryString(im), 16);
				hackFile.append(bCode + '\n');
			} else if (nowLineType == CommandType.C_COMMAND_0) {
				int eqPos = nowLine.indexOf('='), brcPos = nowLine.indexOf(';');
				String bCode = "111";
				bCode += compMap.get(nowLine.substring(eqPos + 1, brcPos));
				bCode += destMap.get(nowLine.substring(0, eqPos));
				bCode += jumpMap.get(nowLine.substring(brcPos + 1));
				hackFile.append(bCode + '\n');
			} else if (nowLineType == CommandType.C_COMMAND_1) {
				int eqPos = nowLine.indexOf('=');
				String bCode = "111";
				bCode += compMap.get(nowLine.substring(eqPos + 1));
				bCode += destMap.get(nowLine.substring(0, eqPos));
				bCode += "000";
				hackFile.append(bCode + '\n');
			} else if (nowLineType == CommandType.C_COMMAND_2) {
				int brcPos = nowLine.indexOf(';');
				String bCode = "111";
				bCode += compMap.get(nowLine.substring(0, brcPos));
				bCode += "000";
				bCode += jumpMap.get(nowLine.substring(brcPos + 1));
				hackFile.append(bCode + '\n');
			} else if (nowLineType == CommandType.C_COMMAND_3) {
				String bCode = "111";
				bCode += compMap.get(nowLine);
				bCode += "000";
				bCode += "000";
				hackFile.append(bCode + '\n');
			} else if (nowLineType == CommandType.NO_COMMAND) {
				invalidSyntax(lineNum);
			}
			if (nowLineType != CommandType.NO_COMMAND && nowLineType != CommandType.L_COMMAND) labelCount++;
		}
		sc.close();
		//System.out.print(hackFile);
	} 
	
	/**
	* return the type of the command
	*/
	private CommandType isCommandType(String line) {
		if (line.charAt(0) == '@' && Pattern.matches(USER_DEFINED, line.substring(1))) {
			return CommandType.A_COMMAND;
		}
		if (line.charAt(0) == '@' && Pattern.matches("^[0-9]+$", line.substring(1))) {
			return CommandType.A_COMMAND_L;
		}
		int len = line.length();
		if(line.charAt(0) == '(' && line.charAt(len - 1) == ')'
				&& Pattern.matches(USER_DEFINED, line.substring(1, len - 1))) {
			return CommandType.L_COMMAND;
		}
		int eqPos = line.indexOf('='), brcPos = line.indexOf(';');
		if(eqPos != -1 && brcPos != -1
				&&destMap.containsKey(line.substring(0, eqPos))
				&& compMap.containsKey(line.substring(eqPos + 1, brcPos))
				&& jumpMap.containsKey(line.substring(brcPos + 1))) {
			return CommandType.C_COMMAND_0;
		}
		if(eqPos != -1
				&& destMap.containsKey(line.substring(0, eqPos))
				&& compMap.containsKey(line.substring(eqPos + 1))) {
			return CommandType.C_COMMAND_1;
		}
		if(brcPos != -1
				&& compMap.containsKey(line.substring(0, brcPos))
				&& jumpMap.containsKey(line.substring(brcPos + 1))) {
			return CommandType.C_COMMAND_2;
		}
		if(compMap.containsKey(line)) {
			return CommandType.C_COMMAND_3;
		}
		return CommandType.NO_COMMAND;
	}
	
	/**
	* pad zeros
	*/
	private String padZero(String s, int pad) {
		int len = s.length();
		String res = "";
		for (int i = 0 ;i < pad - len; i++) {
			res += "0";
		}
		return res + s;
	}
	
	/**
	* point the invalid syntax
	*/
	private void invalidSyntax(int num) {
		System.out.println("Invalid syntax in line " + num + ".");
		System.exit(1);
	}
	
	/**
	* output the hack file
	*/
	private void generateHack() {
		try {
			OutputStream os = new FileOutputStream(filePath + fileName + ".hack");
			byte[] bytes = hackFile.toString().getBytes();
			os.write(bytes);
			os.flush();
			os.close();
		} catch (IOException e){
			System.out.println("Write failed!");
		}
		System.out.println("Success!");
	}
}