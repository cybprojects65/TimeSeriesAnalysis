package it.cnr.timeseries.analysis.datastructures;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class Utils {

	public static void deleteDirectoryWalkTree(Path path) throws IOException {
		  FileVisitor visitor = new SimpleFileVisitor<Path>() {
		             
		    @Override
		    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		      Files.delete(file);
		      return FileVisitResult.CONTINUE;
		    }
		 
		    @Override
		    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		      Files.delete(file);
		      return FileVisitResult.CONTINUE;
		    }
		 
		    @Override
		    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		      if (exc != null) {
		        throw exc;
		      }
		      Files.delete(dir);
		      return FileVisitResult.CONTINUE;
		    }
		  };
		  Files.walkFileTree(path, visitor);
		}
	
}
