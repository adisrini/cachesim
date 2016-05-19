import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class TestGenerator {
	
	private String generate() {
		String text = "";
		String[] types = new String[]{"load 0x", "store 0x"};
		int access = (int) Math.pow(2, (int) (3 * Math.random()));
		String nl = "\n";
		for(int i = 0; i < 10000; i++) {
			String type = types[(int) Math.round(Math.random())];
			String strAddress = Integer.toString((int) ((int) Math.pow(2, 16) * Math.random()));
			if(strAddress.length() > 6) {
				strAddress = strAddress.substring(0, 6);
			} else {
				while(strAddress.length() < 6) {
					strAddress = "0" + strAddress;
				}
			}
			String strAccess = Integer.toString(access);
			StringBuffer sb = new StringBuffer();
			String val = "";
			if(type.contains("store")) {
				Random r = new Random();
		        while(sb.length() < access*2){
		            sb.append(Integer.toHexString(r.nextInt()));
		        }
				val = sb.toString().substring(0, access*2);
			}
			text += type + strAddress + " " + strAccess + " " + val + nl;
		}
		return text;
	}
	
	public static void main(String[] args) {
		try {
			
			TestGenerator tg = new TestGenerator();
			
			String content = tg.generate();

			File file = new File("/Users/adityasrinivasan/Documents/GitHub/cachesim/src/filename");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();

			System.out.println("Done");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}