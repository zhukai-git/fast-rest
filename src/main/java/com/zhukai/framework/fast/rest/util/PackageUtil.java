package com.zhukai.framework.fast.rest.util;

import com.zhukai.framework.fast.rest.exception.PackageRepeatException;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PackageUtil {

	/**
	 * 获取某个包下的所有类
	 */
	public static List<Class> getAllClasses(String packageName) throws Exception {
		List<Class> classes = new ArrayList<>();
		String packageDirName = packageName.replace('.', '/');
		Enumeration<URL> dirs = ClassLoader.getSystemClassLoader().getResources(packageDirName);
		if (dirs.hasMoreElements()) {
			URL url = dirs.nextElement();
			if (dirs.hasMoreElements()) {
				throw new PackageRepeatException(packageName + " is repeated, please change the package name");
			}
			String protocol = url.getProtocol();
			if ("file".equals(protocol)) {
				String filePath = URLDecoder.decode(url.getFile(), "utf-8");
				findClassInPackageByFile(packageName, filePath, classes);
			} else if ("jar".equals(protocol)) {
				try {
					findClassInPackageByJar(packageDirName, url, classes);
				} catch (NoClassDefFoundError error) {
					throw new PackageRepeatException(packageName + " is repeated, please change the package name");
				}
			}
		}
		return classes;
	}

	private static void findClassInPackageByFile(String packageName, String filePath, List<Class> classes) throws Exception {
		File dir = new File(filePath);
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}
		File[] dirFiles = dir.listFiles(file -> file.isDirectory() || file.getName().endsWith("class"));
		if (dirFiles != null) {
			for (File file : dirFiles) {
				if (file.isDirectory()) {
					findClassInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), classes);
				} else {
					String className = file.getName().substring(0, file.getName().length() - 6);
					classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + "." + className));
				}
			}
		}
	}

	private static void findClassInPackageByJar(String packageDirName, URL url, List<Class> classes) throws IOException, ClassNotFoundException {
		JarFile jar = JarURLConnection.class.cast(url.openConnection()).getJarFile();
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName().charAt(0) == '/' ? entry.getName().substring(1) : entry.getName();
			if (name.startsWith(packageDirName) && name.endsWith(".class") && !entry.isDirectory()) {
				int idx = name.lastIndexOf('/');
				if (idx != -1) {
					String packageName = name.substring(0, idx).replace('/', '.');
					String className = name.substring(packageName.length() + 1, name.length() - 6);
					classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
				}
			}
		}
	}

	private PackageUtil() {
	}
}
