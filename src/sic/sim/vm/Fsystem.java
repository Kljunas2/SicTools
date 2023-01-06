package sic.sim.vm;

import java.io.*;

import sic.common.Conversion;
import sic.common.Logger;

/**
 * @author: Miha Korenjak
 */
public class Fsystem {
	protected static int targetFile = 0;
	protected static int address = 0;
	protected static String fileName = null;
	protected static String openedFile = null;
	protected static RandomAccessFile file = null;

	public static class File extends Device {
		@Override
		public int read() {
			return targetFile;
		}

		@Override
		public void write(int value) {
			targetFile = value;
		}
	}

	public static class Address extends Device {
		private int offset; // 0, 1 or 2

		public Address(int offset) {
			this.offset = offset;
		}

		@Override
		public int read() {
			return address >> (2-offset)*8 & 0xff;
		}

		@Override
		public void write(int value) {
			int mask = 0xff << (2-offset)*8;
			address = address &~ mask;
			value <<= (2-offset)*8;
			address |= value;
			//System.out.printf("%06x%n", address);
		}
	}

	public static class Operation extends Device {
		@Override
		public void write(int opcode) {
			byte[] buf;
			int a, s;
			switch (opcode) {
				case 0x00:
					openFile();
					buf = new byte[1];
					try {
						file.seek(address+8);
						file.read(buf);
					} catch (IOException e) {
						Logger.fmterr("Cannot read from file '%s'", fileName);
					}
					Machine.sRegisters.set(Registers.rA, buf[0]);
					address++;
					break;
				case 0x01:
					openFile();
					buf = new byte[3];
					try {
						file.seek(address+8);
						file.read(buf);
					} catch (IOException e) {
						Logger.fmterr("Cannot read from file '%s'", fileName);
					}
					a = 0;
					for (int i = 0; i < 3; i++) {
						a <<= 8;
						a |= buf[i];
					}
					Machine.sRegisters.set(Registers.rA, a);
					address += 3;
					break;
				case 0x02:
					openFile();
					buf = new byte[1];
					buf[0] = (byte) Machine.sRegisters.getS();
					try {
						file.seek(address+8);
						file.write(buf);
					} catch (IOException e) {
						Logger.fmterr("Cannot write to file '%s'", fileName);
					}
					address++;
					break;
				case 0x03:
					openFile();
					buf = new byte[3];
					s = Machine.sRegisters.getS();
					for (int i = 2; i >= 0; i--) {
						buf[i] = (byte) (s & 0xff);
						s >>= 8;
					}
					try {
						file.seek(address+8);
						file.write(buf);
					} catch (IOException e) {
						Logger.fmterr("Cannot write to file '%s'", fileName);
					}
					address += 3;
					break;

				case 0x10:
					openFile();
					int size = 0;
					try {
						size = (int) file.length()-8;
					} catch (IOException e) {
						Logger.fmterr("Cannot read size of file '%s'", fileName);
					}
					Machine.sRegisters.set(Registers.rA, size);
					break;
				case 0x11:
					openFile();
					int pdir = 0;
					try {
						file.seek(0);
						pdir = file.read();
					} catch (IOException e) {
						Logger.fmterr("Cannot read parent of file '%s'", fileName);
					}
					Machine.sRegisters.set(Registers.rA, pdir);
					break;
				case 0x12:
					openFile();
					buf = new byte[6];
					try {
						file.seek(2);
						file.read(buf);
					} catch (IOException e) {
						Logger.fmterr("Cannot read name of file '%s'", fileName);
					}
					a = 0;
					for (int i = 0; i < 3; i++) {
						a <<= 8;
						a |= buf[i];
					}
					s = 0;
					for (int i = 0; i < 3; i++) {
						s <<= 8;
						s |= buf[i+3];
					}
					Machine.sRegisters.set(Registers.rA, a);
					Machine.sRegisters.set(Registers.rS, s);
					break;
				case 0x13:
					boolean t = testFile();
					if (t) {
						Machine.sRegisters.setA(1);
					} else {
						Machine.sRegisters.setA(0);
					}
					break;
				case 0x14:
					openFile();
					int type = 0;
					try {
						file.seek(1);
						type = file.read();
					} catch (IOException e) {
						Logger.fmterr("Cannot read type of file '%s'", fileName);
					}
					Machine.sRegisters.set(Registers.rA, type);
					break;

				case 0x20:
					openFile();
					try {
						file.setLength(8);
					} catch (IOException e) {
						Logger.fmterr("Cannot create new file '%s'", fileName);
					}
					break;
				case 0x21:
					deleteFile();
					break;
				case 0x22:
					openFile();
					buf = new byte[6];
					int tmp = address;
					for (int i = 2; i >= 0; i--) {
						buf[i] = (byte) (tmp & 0xff);
						tmp >>= 8;
					}
					s = Machine.sRegisters.getS();
					for (int i = 2; i >= 0; i--) {
						buf[i+3] = (byte) (s & 0xff);
						s >>= 8;
					}
					try {
						file.seek(2);
						file.write(buf);
					} catch (IOException e) {
						Logger.fmterr("Cannot change name of file '%s'", fileName);
					}
					break;
				case 0x23:
					openFile();
					buf = new byte[1];
					a = Machine.sRegisters.getS();
					buf[0] = (byte) a;
					try {
						file.seek(0);
						file.write(buf);
					} catch (IOException e) {
						Logger.fmterr("Cannot move file '%s'", fileName);
					}
					break;
				case 0x24:
					openFile();
					buf = new byte[1];
					a = Machine.sRegisters.getS();
					buf[0] = (byte) a;
					try {
						file.seek(1);
						file.write(buf);
					} catch (IOException e) {
						Logger.fmterr("Cannot change filetype of '%s'", fileName);
					}
					break;
				case 0x25:
					openFile();
					try {
						file.setLength(address+8);
					} catch (IOException e) {
						Logger.fmterr("Cannot resize file '%s'", fileName);
					}
					break;

				case 0x30:
					Machine.sRegisters.setA(0x100);
					for (int i = 0; i < 256; i++) {
						if (!testFile(getFileName(i))) {
							Machine.sRegisters.setA(i);
							break;
						}
					}
					break;
				case 0x31:
					address = 0;
					break;
				case 0x32:
					address--;
					break;
				case 0x33:
					address -= 3;
					break;
				case 0x34:
					address++;
					break;
				case 0x35:
					address += 3;
					break;
				default:
					Logger.fmterr("file operation %d not implemented", opcode);
			}
		}
	}

	private static String getFileName(int f) {
		return "fsystem/"+Conversion.byteToHex(f);
	}

	private static boolean updateFileName() {
		String newFileName = getFileName(targetFile);
		if (fileName == null || !fileName.equals(newFileName)) {
			fileName = newFileName;
		}
		if (fileName.equals(openedFile)) {
			return false;
		}
		return true;
	}

	private static void openFile() {
		if (updateFileName() || file == null) {
			try {
				if (file != null)
					file.close();
				file = new RandomAccessFile(fileName, "rw");
				openedFile = fileName;
			} catch (FileNotFoundException e) {
				Logger.fmterr("Cannot open file '%s'", fileName);
			} catch (IOException e) {
				// possible resource leak
			}
		}
	}

	private static void deleteFile() {
		updateFileName();
		java.io.File dFile = new java.io.File(fileName);
		dFile.delete();

		fileName = null;
		file = null;
	}

	private static boolean testFile() {
		updateFileName();
		return testFile(fileName);
	}

	private static boolean testFile(String name) {
		java.io.File dFile = new java.io.File(name);
		return dFile.exists();
	}
}
