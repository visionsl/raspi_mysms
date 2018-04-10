package com.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;


public class FileUtil {
	
	/**
	 * 读取指定目录下所有文件
	 * @param dir
	 * @param suffix
	 * @return
	 */
	public static List<File> getFilesByDir(String dir,String suffix){
		File file=new File(dir);
		List<File> list=new ArrayList<File>();
		File[] tempList = file.listFiles();
		System.out.println("files num:"+tempList.length);
		for (int i = 0; i < tempList.length; i++) {
			if(tempList[i].isFile() && tempList[i].getName().endsWith(suffix)){
				list.add(tempList[i]);
			}
		}
		return list;
	}
	
	/**
	 * 返回文件扩展名，不含“.”
	 * @param 文件全名称
	 * @return
	 */
	public static String GetFileExt(String filepath){
		 if (filepath!=null&&!filepath.equals("")){
			  if (filepath.lastIndexOf(".") > 0){
		            return filepath.substring(filepath.lastIndexOf(".") + 1); //文件扩展名，不含“.”
		      }
	     }
		 return "";
	}
	/**
	 * 获取网站根路径(根据文件获取)
	 * 在Linux下面文件目录前面也要“/” 如："E:/Txx/project/hotelcms/WebRoot/"
	 * FileUtil.class.getProtectionDomain().getCodeSource().getLocation().getFile()
	 * E:/Txx/project/hotelcms/WebRoot/
	 */
	public static String getRootPathByFile(){
		String path = FileUtil.class.getProtectionDomain().getCodeSource().getLocation().getFile();
		if(path.indexOf("WEB-INF")!=-1)path = path.substring(0,path.indexOf("WEB-INF"));
		if(path.substring(0,1).equals("/"))path=path.substring(1);
		return path;
	}
	
	public static boolean fileExists(String file){
		File f = new File(file);
		return f.exists();
	}
	/**
	 * 根据指定目录及文件名生成文件
	 * @param path		"c:/xx/xx/"
	 * @param fileName	文件名"aa.xml"，不存在则创建
	 * @param content	内容
	 * @param isReplace	文件存在时，false:追加 或 true:替换
	 */
	public static boolean writeFile(String path,String fileName,String content,boolean isReplace){
		newDirectory(path);
		String statsLogFile =  path + fileName;
		RandomAccessFile bw = null;
		try {
			File f = new File(statsLogFile);
			if(f.exists()){
				if(isReplace){
					f.delete();//删除旧的重新创建
					bw = new RandomAccessFile(f, "rw");
				}else{
					bw = new RandomAccessFile(f,"rw");
					bw.seek(bw.length());//将指针移动到文件末尾
				}
			}else{
				bw = new RandomAccessFile(f, "rw");
			}
			byte[] strBytes = content.getBytes("UTF-8");
			bw.write(strBytes);
		} catch (Exception es) {
			System.out.print("IO读写异常!");
			es.printStackTrace();
			return false;
		} finally {
			try {
				if(bw!=null)bw.close();
			} catch (IOException e) {
				System.out.print("资源回收失败!");
				e.printStackTrace();
			}
		}
		return true;
	}
	
	/**
	 * 目录不存在，则新建目录
	 * @param path	"c:/xx/xx/"
	 */
	public static void newDirectory(String path) {
		File saveDirFile = new File( path );
        if (!saveDirFile.exists()) {
            saveDirFile.mkdirs();
        }
	}
	/**
	 * 删除文件
	 * @param pathAndFileName		"c:/xx/xx/aa.xml"
	 */
	public static void delFile(String pathAndFileName) {
		File myDelFile = new File(pathAndFileName);
		if (myDelFile.exists() == true && myDelFile.isFile()) {
			myDelFile.delete();
			System.out.println(pathAndFileName+":del success");
		}
	}
	/**
	 * 删除文件夹
	 * @param path	"c:/xx/xx/"
	 */
	public static void delDirectory(String path) {
		delAllFile(path); // 删除完里面所有内容
		File myFilePath = new File(path);
		if(myFilePath.exists() == true){
			myFilePath.delete(); // 删除空文件夹
		}
	}
	/**
	 * 删除文件夹里面的所有文件
	 * @param path	"c:/xx/xx/"
	 */
	public static void delAllFile(String path) {
		File file = new File(path);
		if (!file.exists()) 		return;
		if (!file.isDirectory()) 	return;
		String[] tempList = file.list();
		File temp = null;
		for (int i = 0; i < tempList.length; i++) {
			temp = new File(path + tempList[i]);
			if (temp.isFile())		temp.delete();
			if (temp.isDirectory()) {
				delAllFile(path + tempList[i]);// 先删除文件夹里面的文件
				delDirectory(path + tempList[i]);// 再删除空文件夹
			}
		}
	}
	/**
	 * 读取文件内容
	 * @param fileName
	 * @return
	 */
	public static String getFileContent(String fileName){
		StringBuffer sb = new StringBuffer();
		File myFile = new File(fileName);
		if (!myFile.exists()) {
			System.err.println("Can't Find " + fileName);
			return "";
		}
		
		try {
			//BufferedReader in = new BufferedReader(new FileReader(myFile));
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(myFile),"UTF-8"));
			String str;
			while ((str = in.readLine()) != null) {
				//System.out.println(str);
				sb.append(str);
			}
			in.close();
		} catch (IOException e) {
			e.getStackTrace();
		}
		
		return sb.toString();
	}
}

