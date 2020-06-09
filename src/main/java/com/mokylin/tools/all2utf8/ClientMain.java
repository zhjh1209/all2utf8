package com.mokylin.tools.all2utf8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ClientMain {
	final static Charset UTF_8 = Charset.forName("UTF-8");
	final static Charset GB2312 = Charset.forName("GB2312");
	final static Charset GB18030 = Charset.forName("GB18030");
	final static BytesEncodingDetect detect = new BytesEncodingDetect();

	public static void main(String[] args) {
		String ext = ".txt";
		List<String> sourceDirs = new ArrayList<>();
		if (args.length >= 2) {
			ext = args[0];
			for (int i = 1; i < args.length; i++) {
				sourceDirs.add(args[i]);
			}
		} else {
			sourceDirs.add(".");
		}
		List<String> errorMsgs = new LinkedList<>();
		List<String> normalMsgs = new LinkedList<>();
		List<String> successMsgs = new LinkedList<>();
		for (String sourceDir : sourceDirs) {
			File file = new File(sourceDir);
			if (!file.exists()) {
				errorMsgs.add("指定的路径不存在：" + sourceDir);
				continue;
			}
			if (!file.isDirectory()) {
				errorMsgs.add("指定的路径必不是文件夹：" + sourceDir);
				continue;
			}
			try {
				transform(file, ext, successMsgs, errorMsgs, normalMsgs);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("===================== 转换结果 ==========================");
		System.out.println("提示信息：");
		for (String normalMsg : normalMsgs) {
			System.out.println(normalMsg);
		}
		System.out.println("======================================================");
		System.out.println("成功信息：");
		for (String successMsg : successMsgs) {
			System.out.println(successMsg);
		}
		System.out.println("======================================================");
		System.out.println("错误信息：");
		for (String errorMsg : errorMsgs) {
			System.out.println(errorMsg);
		}
	}

	static void transform(File file, String ext, List<String> successMsgs, List<String> errorMsgs,
			List<String> normalMsgs) {
		if (!file.isDirectory()) {
			throw new RuntimeException("必须是文件夹");
		}
		List<File> files = getAllFiles(file);
		for (File normalFile : files) {
			String filePath = normalFile.getAbsolutePath();
			try {
				Charset charset = null;
				if (isMatch(ext, filePath)) {
					if ((charset = detectCharset(normalFile)) != null) {
						if (!charset.equals(UTF_8)) {
							// 由于GB2312是GB18030的子集，所以这里可能会有问题，用GB18030读就可以了
							Reader in = new BufferedReader(new InputStreamReader(new FileInputStream(filePath),
									GB2312.equals(charset) ? GB18030 : charset));
							File target = new File(filePath + ".tmp");
							Writer out = new BufferedWriter(
									new OutputStreamWriter(new FileOutputStream(target), UTF_8));

							char[] transcodeBuffer = new char[1024];
							int toRead = transcodeBuffer.length;
							int len;
							while ((len = in.read(transcodeBuffer, 0, toRead)) != -1) {
								out.write(transcodeBuffer, 0, len);
							}
							in.close();
							out.close();
							normalFile.delete();
							target.renameTo(normalFile);
							successMsgs.add("文件编码：" + charset.name() + " 转换成功：" + filePath);
						} else {
							normalMsgs.add("文件编码：" + charset.name() + " 不需转换：" + filePath);
						}
					} else {
						errorMsgs.add("文件编码：" + (charset == null ? "未知" : charset.name()) + " 无法转换：" + filePath);
					}
				}
			} catch (Exception e) {
				errorMsgs.add("文件转换失败：" + filePath);
				e.printStackTrace();
			}
		}
	}

	private static Charset detectCharset(File file) {
		String encode = BytesEncodingDetect.javaname[detect.detectEncoding(file)];
		try {
			return Charset.forName(encode);
		} catch (UnsupportedCharsetException e) {

		}
		return null;
	}

	private static boolean isMatch(String ext, String filePath) {
		return filePath.endsWith(ext.toLowerCase()) || filePath.endsWith(ext.toUpperCase());
	}

	static List<File> getAllFiles(File file) {
		List<File> files = new LinkedList<>();
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			for (File child : children) {
				if (child.isDirectory()) {
					files.addAll(getAllFiles(child));
				} else {
					files.add(child);
				}
			}
		} else {
			files.add(file);
		}
		return files;
	}
}
