package filesystemtransaction;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileSystem {
	
	protected final FileSystemModel model;
	
	protected final Lock wlock;
	
	protected final Lock rlock;
	
	public FileSystem(FileSystemModel model) throws IOException {
		this.model = model;
		if (model.existsTransaction()) {
			new TransactionFile().restore();
		}
		ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
		this.rlock = rwlock.readLock();
		this.wlock = rwlock.writeLock();
	}
	
	public ReaderTransactionFileSystem read() {
		this.rlock.lock();
		return new ReaderTransactionFileSystem(rlock);
	}
	
	public WriteTransactionFileSystem write() {
		this.wlock.lock();
		return new WriteTransactionFileSystem(wlock);
	}
	
	protected class TransactionFile {
		
		protected final Set<String> delete;
		
		protected final Map<String, byte[]> create;
		
		protected final Map<String, byte[]> write;
		
		/**
		 * @param delete
		 * @param create
		 * @param write
		 */
		public TransactionFile(Set<String> delete, Map<String, byte[]> create, Map<String, byte[]> write) {
			super();
			this.delete = delete;
			this.create = create;
			this.write = write;
		}
		
		public TransactionFile() throws IOException {
			try (DataInputStream in = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(model.readTransaction())))) {
				delete = new HashSet<>();
				int deleteCount = in.readInt();
				for (int n = 0; n < deleteCount; n++) {
					delete.add(in.readUTF());
				}
				create = new HashMap<>();
				int createdCount = in.readInt();
				for (int n = 0; n < createdCount; n++) {
					String path = in.readUTF();
					byte[] bytes = new byte[in.readInt()];
					in.readFully(bytes);
					create.put(path, bytes);
				}
				write = new HashMap<>();
				int writedCount = in.readInt();
				for (int n = 0; n < writedCount; n++) {
					String path = in.readUTF();
					byte[] bytes = new byte[in.readInt()];
					in.readFully(bytes);
					write.put(path, bytes);
				}
			}
		}
		
		public void write() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(bytes))) {
				out.writeInt(delete.size());
				for (String path : delete) {
					out.writeUTF(path);
				}
				out.writeInt(create.size());
				for (Entry<String, byte[]> entry : create.entrySet()) {
					out.writeUTF(entry.getKey());
					out.writeInt(entry.getValue().length);
					out.write(entry.getValue());
				}
				out.writeInt(write.size());
				for (Entry<String, byte[]> entry : write.entrySet()) {
					out.writeUTF(entry.getKey());
					out.writeInt(entry.getValue().length);
					out.write(entry.getValue());
				}
			}
			model.writeTransaction(bytes.toByteArray());
		}
		
		public void restore() throws IOException {
			for (String path : delete) {
				model.delete(path);
			}
			for (Entry<String, byte[]> entry : create.entrySet()) {
				model.write(entry.getKey(), entry.getValue());
			}
			for (Entry<String, byte[]> entry : write.entrySet()) {
				model.write(entry.getKey(), entry.getValue());
			}
		}
		
		public void delete() throws IOException {
			model.deleteTransaction();
		}
	}
	
	public class ReaderTransactionFileSystem implements AutoCloseable {
		
		protected final Lock lock;
		
		protected final Map<String, byte[]> pathReaded = new HashMap<>();
		
		protected final Set<String> pathExists = new HashSet<>();
		
		/**
		 * @param lock
		 */
		public ReaderTransactionFileSystem(Lock lock) {
			super();
			this.lock = lock;
		}
		
		public boolean exists(String path) throws IOException {
			if (pathExists.contains(path)) { return true; }
			return model.exists(path);
		}
		
		public byte[] read(String path) throws FileNotFoundException, IOException {
			if (pathReaded.containsKey(path)) { return pathReaded.get(path); }
			if (path.contains("..")) { throw new IllegalArgumentException(path); }
			return model.read(path);
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void close() throws IOException {
			lock.unlock();
		}
	}
	
	public class WriteTransactionFileSystem extends ReaderTransactionFileSystem {
		
		protected final Set<String> pathToDelete = new HashSet<>();
		
		protected final Map<String, byte[]> writed = new HashMap<>();
		
		public WriteTransactionFileSystem(Lock lock) {
			super(lock);
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean exists(String path) throws IOException {
			if (pathToDelete.contains(path)) { return false; }
			if (writed.containsKey(path)) { return true; }
			return super.exists(path);
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public byte[] read(String path) throws FileNotFoundException, IOException {
			if (pathToDelete.contains(path)) { throw new FileNotFoundException(path); }
			if (writed.containsKey(path)) { return writed.get(path); }
			return super.read(path);
		}
		
		public void write(String path, byte[] bytes) {
			writed.put(path, bytes);
			pathToDelete.remove(path);
		}
		
		public void delete(String path) {
			pathToDelete.add(path);
			writed.remove(path);
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void close() throws IOException {
			try {
				Map<String, byte[]> bytesToCreate = new HashMap<>();
				for (String path : pathToDelete) {
					bytesToCreate.put(path, model.read(path));
				}
				Map<String, byte[]> bytesToWrite = new HashMap<>();
				Set<String> bytesToRemove = new HashSet<>();
				for (String path : writed.keySet()) {
					if (model.exists(path)) {
						bytesToWrite.put(path, model.read(path));
					} else {
						bytesToRemove.add(path);
					}
				}
				TransactionFile transactionFile = new TransactionFile(bytesToRemove, bytesToCreate, bytesToWrite);
				try {
					transactionFile.write();
					for (String path : pathToDelete) {
						model.delete(path);
					}
					for (Entry<String, byte[]> entry : writed.entrySet()) {
						model.write(entry.getKey(), entry.getValue());
					}
					transactionFile.delete();
				} catch (Throwable e) {
					transactionFile.restore();
					transactionFile.delete();
				}
			} finally {
				lock.unlock();
			}
		}
	}
}
