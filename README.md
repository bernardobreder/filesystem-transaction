# Filesystem Transaction

O objetivo do modulo é criar um sistema de arquivo cuja as mudanças sejam atomicas e transacionais.

```java
FileSystemModel model = ...
FileSystem fs = new FileSystem(model);

try (WriteTransactionFileSystem write = fs.write()) {
    write.write("a", BYTES_1);
    write.write("b", BYTES_2);
    write.write("c", BYTES_3);
}

try (ReaderTransactionFileSystem read = fs.read()) {
    assertArrayEquals(BYTES_1, read.read("a"));
    assertArrayEquals(BYTES_2, read.read("b"));
    assertArrayEquals(BYTES_3, read.read("c"));
    assertTrue(read.exists("a"));
    assertTrue(read.exists("b"));
    assertTrue(read.exists("c"));
    assertFalse(read.exists("d"));
}

try (WriteTransactionFileSystem write = fs.write()) {
    write.delete("b");
}

try (ReaderTransactionFileSystem read = fs.read()) {
    assertTrue(read.exists("a"));
    assertFalse(read.exists("b"));
    assertTrue(read.exists("c"));
}
```

Para utilizar o sistema de arquivo transacional, é preciso criar um modelo com a implementação da persistencia dos dados

```java
/**
 * Modelo do sistema de arquivo
 */
public interface FileSystemModel {

    /**
     * Escreve o path com o conteudo em bytes
     *
     * @param path
     * @param bytes
     * @throws IOException
     */
    public void write(String path, byte[] bytes) throws IOException;
    
    /**
     * Lê o conteúdo em bytes
     *
     * @param path
     * @return bytes
     * @throws IOException
     */
    public byte[] read(String path) throws IOException;
    
    /**
     * Indica se o path existe
     *
     * @param path
     * @return existe
     * @throws IOException
     */
    public boolean exists(String path) throws IOException;

    /**
     * Deleta o path
     *
     * @param path
     * @throws IOException
     */
    public void delete(String path) throws IOException;

    /**
     * Escreve o arquivo transacional com os bytes
     *
     * @param bytes
     * @throws IOException
     */
    public void writeTransaction(byte[] bytes) throws IOException;
    
    /**
     * Realiza a leitura do arquivo transacional, sabendo que ele existe
     *
     * @return bytes transacional
     * @throws IOException
     */
    public byte[] readTransaction() throws IOException;
    
    /**
     * Indica se o arquivo transacional existe
     *
     * @return existe
     * @throws IOException
     */
    public boolean existsTransaction() throws IOException;
    
    /**
     * Remove o arquivo transacional
     *
     * @throws IOException
     */
    public void deleteTransaction() throws IOException;
    
}
``