import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.Queue;

public class Cache {

	File inFile;
	int memorySize;
	int cacheSize;
	int associativity;
	int blockSize;
	int numberOfSets;
	int addressSize;
	int offsetSize;
	int setIndexSize;
	int tagSize;

	Set[] cache;
	String[] memory;

	public static void main(String args[]) {
		Cache main = new Cache();
		main.start(args[0], args[1], args[2], args[3]);
	}

	public void start(String textFile, String cacheSize, String associativity, String blockSize) {
		this.addressSize = 24;
		this.memorySize = 16;
		this.inFile = new File(textFile);
		this.cacheSize = Integer.parseInt(cacheSize);
		this.associativity = Integer.parseInt(associativity);
		this.blockSize = Integer.parseInt(blockSize);
		this.numberOfSets = (1024 * this.cacheSize) / (this.associativity * this.blockSize);
		this.offsetSize = (int) log(this.blockSize, 2);
		this.setIndexSize = (int) log(this.numberOfSets, 2);
		this.tagSize = addressSize - this.offsetSize - this.setIndexSize;
		initializeMemory();
		initializeCache();
		loadTrace();
	}

	void initializeMemory() {
		this.memory = new String[(int) Math.pow(2, 24)];
		for (int i = 0; i < memory.length; i++) {
			memory[i] = "00";
		}
	}

	void initializeCache() {
		cache = new Set[this.numberOfSets];
		for (int i = 0; i < cache.length; i++) {
			cache[i] = new Set(associativity);
		}
	}

	void loadTrace() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inFile));
			while (br.ready()) {
				Instruction instr = new Instruction(br.readLine());
				handleInstruction(instr);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void handleInstruction(Instruction ins) {
		if (ins.instrType == InstructionType.LOAD) {
			load(ins);
		} else {
			store(ins);
		}
	}

	void load(Instruction ins) {
		int index = ins.getIndex();
		Set set = cache[index];
		set.load(ins);
	}

	void store(Instruction ins) {
		int index = ins.getIndex();
		Set set = cache[index];
		set.store(ins);
	}

	double log(double x, int base) {
		return (Math.log(x) / Math.log(base));
	}

	class Block {
		private int size;
		private boolean valid;
		private int tag;
		private String[] bytes;

		public Block(int blockSize) {
			this.size = blockSize;
			this.tag = Integer.MIN_VALUE;
			this.valid = false;
			this.bytes = new String[this.size];
			for (int i = 0; i < this.size; i++) {
				bytes[i] = "00";
			}
		}

		boolean isValid() {
			return valid;
		}

		void setValid(boolean v) {
			this.valid = v;
		}

		int getTag() {
			return this.tag;
		}

		void setTag(int tag) {
			this.tag = tag;
		}

		String[] getBytes() {
			return this.bytes;
		}

		void display() {
			for (int i = 0; i < this.bytes.length; i++) {
				System.out.println(this.bytes[i]);
			}
		}

		String getBytesInRange(int start, int end) {
			String result = "";
			for (int i = start; i < end; i++) {
				result += bytes[i];
			}
			return result;
		}

		void setBytesInRange(int start, String newBytes) {
			String[] newByteArray = new String[newBytes.length() / 2];

			for (int i = 0; i < newByteArray.length; i++) {
				newByteArray[i] = newBytes.substring(i * 2, (i + 1) * 2);
			}

			int newCount = 0;
			for (int i = start; (i < start + newByteArray.length) && (i < 32); i++) {
				bytes[i] = newByteArray[newCount];
				newCount++;
			}
		}

	}

	class Frame {
		private Block myBlock;

		public Frame(Block b) {
			this.myBlock = b;
		}

		Block getBlock() {
			return this.myBlock;
		}

	}

	String padWithZeros(String s) {
		while (s.length() < addressSize) {
			s += "0" + s;
		}
		return s;
	}

	String padWithEnoughZeros(String s) {
		while ((s.length() % 2) != 0) {
			s += "0" + s;
		}
		return s;
	}

	class Set {
		private Queue<Frame> myFrames;

		public Set(int associativity) {
			this.myFrames = new LinkedList<Frame>();

			// fill frames with empty blocks
			while (myFrames.size() < associativity) {
				myFrames.offer(new Frame(new Block(blockSize)));
			}

		}

		void load(Instruction ins) {
			System.out.print("load " + ins.getHexAddress());
			Block read = null;

			int tag = (int) ins.getTag();
			int offset = ins.getOffset();
			int access = ins.accessSize;

			if (containsBlockByTag(tag)) {
				if (getBlock(tag).isValid()) {
					System.out.print(" hit ");
					read = getBlock(tag);
					String data = read.getBytesInRange(offset, offset + access);
					System.out.println(data);
					removeFromFrame(tag);
					Frame frame = new Frame(read);
					myFrames.offer(frame);
				}
			} else {
				System.out.print(" miss ");
				// TODO: take from memory to cache and evict
				String fetchedData = fetchFromMem(ins);
				System.out.println((fetchedData).substring(offset * 2, offset * 2 + access * 2));
				Block newBlock = new Block(blockSize);
				newBlock.setBytesInRange(0, fetchedData);
				newBlock.setValid(true);
				newBlock.setTag(tag);
				Frame frame = new Frame(newBlock);
				myFrames.poll();
				myFrames.offer(frame);
			}

		}

		void removeFromFrame(int tag) {
			for (Frame frame : myFrames) {
				if (frame.getBlock().getTag() == tag) {
					myFrames.remove(frame);
					break;
				}
			}
		}

		void store(Instruction ins) {
			System.out.print("store " + ins.getHexAddress());
			Block write = null;

			int tag = (int) ins.getTag();
			int offset = ins.getOffset();

			if (containsBlockByTag(tag)) {
				if (getBlock(tag).isValid()) {
					write = getBlock(tag);
					System.out.println(" hit");
					write.setBytesInRange(offset, ins.value);
					writeToMem(ins);
					removeFromFrame(tag);
					Frame frame = new Frame(write);
					myFrames.offer(frame);
				}
			} else {
				System.out.println(" miss");
				writeToMem(ins);
			}

		}

		void writeToMem(Instruction ins) {
			int index = (int) ins.address;
			String data = ins.value;
			String[] dataSplit = new String[ins.accessSize];
			while (data.length() < ins.accessSize * 2) {
				data = "0" + data;
			}
			for (int i = 0; i < ins.accessSize; i++) {
				dataSplit[i] = data.substring(i * 2, (i + 1) * 2);
			}

			int dataCount = 0;
			for (int i = index; i < index + ins.accessSize; i++) {
				memory[i] = dataSplit[dataCount];
				dataCount++;
			}

		}

		String fetchFromMem(Instruction ins) {
			int index = ins.address;
			while ((index % blockSize) != 0) {
				index--;
			}
			String data = "";
			for (int i = index; i < index + blockSize; i++) {
				data += memory[i];
			}
			return data;
		}

		public boolean containsBlockByTag(int tag) {
			for (Frame frame : myFrames) {
				if (frame.getBlock().getTag() == tag && frame.getBlock().isValid()) {
					return true;
				}
			}
			return false;
		}

		Block getBlock(int tag) {
			Block b = null;
			for (Frame frame : myFrames) {
				if (frame.getBlock().getTag() == tag && frame.getBlock().isValid()) {
					b = frame.getBlock();
					break;
				}
			}
			return b;
		}

	}

	class Instruction {

		InstructionType instrType;
		String hexAddress;
		String binaryAddress;
		int address;
		int accessSize;
		String value;

		public Instruction(String line) {
			String[] split = line.split(" ");
			instrType = InstructionType.valueOf(split[0].toUpperCase());
			hexAddress = split[1];
			address = Integer.parseInt(split[1].substring(2), 16);
			accessSize = Integer.parseInt(split[2]);
			binaryAddress = Integer.toBinaryString(address);
			while (binaryAddress.length() < addressSize) {
				binaryAddress = "0" + binaryAddress;
			}
			if (split.length == 4) {
				value = split[3];
			}
		}

		int getTag() {
			String tag = binaryAddress.substring(0, tagSize);
			return Integer.parseInt(tag, 2);
		}

		int getIndex() {
			String index = binaryAddress.substring(tagSize, tagSize + setIndexSize);
			if (index.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(index, 2);
		}

		int getOffset() {
			String offset = binaryAddress.substring(tagSize + setIndexSize);
			if (offset.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(offset, 2);
		}

		String getHexAddress() {
			return this.hexAddress;
		}

	}

	enum InstructionType {
		LOAD, STORE;
	}

}
