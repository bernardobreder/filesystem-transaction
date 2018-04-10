# Filesystem Transaction

O objetivo do modulo é criar um sistema de arquivo cuja as mudanças sejam atomicas e transacionais.

```java
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