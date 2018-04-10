package filesystemtransaction;

import java.io.IOException;

public interface FileSystemTransactionModel {
	
	public void write(String path, byte[] bytes) throws IOException;

	public byte[] read(String path) throws IOException;

	public boolean exists(String path) throws IOException;
	
	public void delete(String path) throws IOException;
	
	public void writeTransaction(byte[] bytes) throws IOException;

	public byte[] readTransaction() throws IOException;

	public boolean existsTransaction() throws IOException;

	public void deleteTransaction() throws IOException;
}
