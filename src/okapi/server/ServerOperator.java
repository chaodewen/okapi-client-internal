package okapi.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import okapi.client.ClientOperator;

public class ServerOperator {
	
	public static void writeByteBuffer(String fileName, String location, ByteBuffer bb) throws IOException {
			File file = new File(location + fileName);
			FileChannel writeChannel;
			FileOutputStream fos = new FileOutputStream(file, false);
			writeChannel = fos.getChannel();
			writeChannel.write(bb);
			writeChannel.close();
			fos.close();
	}
	public static void unZip(String zipPath, String descDir) throws ZipException, IOException {
		File zipFile = new File(zipPath);
//		String fileName = zipFile.getName();
//		descDir += fileName.substring(0, fileName.lastIndexOf(".")) + File.separator;
			ZipFile zip = new ZipFile(zipFile);
			for(Enumeration<? extends ZipEntry> entries = zip.entries();entries.hasMoreElements();) {
				ZipEntry entry = (ZipEntry)entries.nextElement();
				String zipEntryName = entry.getName();
				InputStream in = zip.getInputStream(entry);
				String outPath = (descDir + zipEntryName).replaceAll("\\*", "/");;
				//判断路径是否存在,不存在则创建文件路径
				File file = new File(outPath.substring(0, outPath.lastIndexOf('/')));
				if(!file.exists()){
					file.mkdirs();
				}
				//判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
				if(new File(outPath).isDirectory()){
					continue;
				}
				//输出文件路径信息
				System.out.println(outPath);
				
				OutputStream out = new FileOutputStream(outPath);
				byte[] buf1 = new byte[1024];
				int len;
				while((len=in.read(buf1))>0){
					out.write(buf1,0,len);
				}
				in.close();
				out.close();
			}
			zip.close();
			System.out.println("解压完毕！");
	}
	
	static class FileSelector implements FilenameFilter {
		String extension;
		FileSelector(String extension) {
			this.extension = extension;
		}
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(extension);
		}
	}
	public static File[] getFiles(String selector, String path) {
		File dir = new File(path);
		return dir.listFiles(new FileSelector(selector));
	}

	public static boolean isCompitable(String def, String target) {
		String[] defPart = ClientOperator.separateURI(def);
		String[] targetPart = ClientOperator.separateURI(target);
		if(defPart.length == targetPart.length) {
			for(int i = 0; i < defPart.length; i++) {
				if(defPart[i].length() == 0 || targetPart[i].length() == 0) {
					return false;
				}
				if(defPart[i].matches("<[a-zA-Z][\\w]+:[\\w-?%&={}]+>")) {
					String type = defPart[i].substring(1, defPart[i].indexOf(":"));
					String value = targetPart[i];
					if(type.equals("boolean") && value.matches("true|false")) {
						try {
							Boolean.parseBoolean(value);
							continue;
						}
						catch (NumberFormatException e) {
							System.out.println("boolean类型错误！");
							return false;
						}
					}
					else if(type.equals("String")) {
						continue;
					}
					else if(type.equals("int") && value.matches("\\d+")) {
						try {
							Integer.parseInt(value);
							continue;
						}
						catch (NumberFormatException e) {
							System.out.println("int类型错误！");
							return false;
						}
					}
					else if(type.equals("double") && value.matches("\\d+(.\\d+)?")) {
						try {
							Double.parseDouble(value);
							continue;
						}
						catch (NumberFormatException e) {
							System.out.println("double类型错误！");
							return false;
						}
					}
					else if(type.equals("float") && value.matches("\\d+(.\\d+)?")) {
						try {
							Float.parseFloat(value);
							continue;
						}
						catch (NumberFormatException e) {
							System.out.println("float类型错误！");
							return false;
						}
					}
					else {
						return false;
					}
				}
				else if(!defPart[i].equals(targetPart[i])){
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}
	public static Object[] getArg( String def, String api_path_last) {
		ArrayList<Object> al = new ArrayList<Object>();
		String[] defPart = ClientOperator.separateURI(def);
		String[] lastPart = ClientOperator.separateURI(api_path_last);
		for(int i = 0; i < defPart.length; i++) {
			if(defPart[i].matches("<[a-zA-Z][\\w]+:[\\w-?%&={}]+>")) {
				String type = defPart[i].substring(1, defPart[i].indexOf(":"));
				String value = lastPart[i];
				if(type.equals("boolean") && value.matches("true|false")) {
					al.add(Boolean.valueOf(value));
				}
				else if(type.equals("String")) {
					al.add(value);
				}
				else if(type.equals("int") && value.matches("\\d+")) {
					al.add(Integer.valueOf(value));
				}
				else if(type.equals("double") && value.matches("\\d+(.\\d+)?")) {
					al.add(Double.valueOf(value));
				}
				else if(type.equals("float") && value.matches("\\d+(.\\d+)?")) {
					al.add(Float.parseFloat(value));
				}
			}
		}
		return al.toArray();
	}
	
}
