package org.yostane.ps2.isorenamer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pmw.tinylog.Logger;

import com.github.stephenc.javaisotools.loopfs.iso9660.Iso9660FileEntry;
import com.github.stephenc.javaisotools.loopfs.iso9660.Iso9660FileSystem;

public class Main {

	public static void main(String[] args) {
		// list all iso files
		File[] isoFiles = new File(".").listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				if (pathname.getName().toLowerCase().endsWith(".iso")) {
					return true;
				}
				return false;
			}
		});

		// process iso files one by one
		for (File file : isoFiles) {
			try {
				renameISOAtPath(file.getAbsolutePath());
			} catch (RuntimeException e) {
				Logger.warn("Failed to process iso file: " + file.getName() + ". Reason: " + e.getMessage());
			}
		}
	}

	/**
	 * @param filePath
	 */
	public static boolean renameISOAtPath(String filePath) throws RuntimeException {
		File isoFile = new File(filePath);
		Iso9660FileSystem image = null;

		// open the image
		Logger.info("Opening image " + filePath);
		try {
			image = new Iso9660FileSystem(isoFile, true);
		} catch (IOException e) {
			Logger.error("Could not open the image file " + filePath);
			return false;
		}


		// find the file with the game identifier
		String gameId = null;
		for (Iso9660FileEntry entry : image) {
			if (entry.isDirectory() == false) {
				// four letters, then a _, then four digits, then a ., then
				// 2
				// digits
				Pattern pattern = Pattern.compile("[A-Z]{4}?_[0-9]{3}?\\.[0-9]{2}?");
				Matcher matcher = pattern.matcher(entry.getName());
				if (matcher.matches()) {
					gameId = entry.getName();
					Logger.info("Found the id " + gameId);
					break;
				}
			}
		}

		// close the image
		try {
			image.close();
		} catch (IOException e) {
			Logger.warn("Failed to close the image " + isoFile.getName());
		}

		if (gameId == null) {
			Logger.warn("game id not found");
			return false;
		}

		// build the prefix
		String gamePrefix = gameId + ".";

		// check if the file is not already renamed
		if (isoFile.getName().startsWith(gamePrefix)) {
			Logger.info("The iso has already the correct prefix");
			return true;
		}

		// append the game id to the name of the iso
		String newName = gameId + "." + isoFile.getName();
		Path source = Paths.get(isoFile.getAbsolutePath());
		try {
			Files.move(source, source.resolveSibling(newName));
		} catch (IOException e) {
			Logger.warn("Failed to rename the iso image");
			return false;
		}

		// success
		Logger.info("File " + isoFile.getName() + " renamed to " + newName);
		return true;
	}
}
