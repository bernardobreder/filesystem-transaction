package filesystemtransaction;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import filesystemtransaction.FileSystemTransaction;
import filesystemtransaction.FileSystemTransactionModel;
import filesystemtransaction.FileSystemTransaction.ReaderTransactionFileSystem;
import filesystemtransaction.FileSystemTransaction.WriteTransactionFileSystem;

public class FileSystemTransactionTest {
	
	@Test
	public void test() throws IOException {
		MemoryFileSystemModel model = new MemoryFileSystemModel();
		FileSystemTransaction fs = new FileSystemTransaction(model);
		try (WriteTransactionFileSystem write = fs.write()) {
			write.write("a", BYTES_1);
			write.write("b", BYTES_2);
			write.write("c", BYTES_3);
		}
		assertEquals(3, model.bytes.size());
		assertNull(model.transaction);
		try (ReaderTransactionFileSystem read = fs.read()) {
			assertArrayEquals(BYTES_1, read.read("a"));
			assertArrayEquals(BYTES_2, read.read("b"));
			assertArrayEquals(BYTES_3, read.read("c"));
			assertTrue(read.exists("a"));
			assertTrue(read.exists("b"));
			assertTrue(read.exists("c"));
			assertFalse(read.exists("d"));
		}
		assertEquals(3, model.bytes.size());
		assertNull(model.transaction);
		try (WriteTransactionFileSystem write = fs.write()) {
			write.delete("b");
		}
		assertEquals(2, model.bytes.size());
		assertNull(model.transaction);
	}
	
	private static final byte[] BYTES_3 = new byte[] {
			3
	};
	
	private static final byte[] BYTES_2 = new byte[] {
			2
	};
	
	private static final byte[] BYTES_1 = new byte[] {
			1
	};
	
	private final class MemoryFileSystemModel implements FileSystemTransactionModel {
		
		private byte[] transaction;
		
		private Map<String, byte[]> bytes = new HashMap<>();
		
		@Override
		public void writeTransaction(byte[] bytes) throws IOException {
			transaction = bytes;
		}
		
		@Override
		public void write(String path, byte[] bytes) throws IOException {
			this.bytes.put(path, bytes);
		}
		
		@Override
		public byte[] readTransaction() throws IOException {
			return transaction;
		}
		
		@Override
		public byte[] read(String path) throws IOException {
			return this.bytes.get(path);
		}
		
		@Override
		public boolean existsTransaction() throws IOException {
			return transaction != null;
		}
		
		@Override
		public boolean exists(String path) throws IOException {
			return this.bytes.containsKey(path);
		}
		
		@Override
		public void deleteTransaction() throws IOException {
			this.transaction = null;
		}
		
		@Override
		public void delete(String path) throws IOException {
			this.bytes.remove(path);
		}
	}
}
