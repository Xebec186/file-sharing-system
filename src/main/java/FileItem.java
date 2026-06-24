/**
 * FileItem represents a metadata entry for files on the sharing system.
 * It is modeled as a Java Record, which automatically generates boilerplate code
 * (constructor, equals, hashCode, toString) for immutable data.
 * 
 * @param name        The name of the file
 * @param size        The formatted file size string (e.g. "1.2 MB" or "Remote File" if unknown)
 * @param sizeInBytes The exact file size in bytes
 */
public record FileItem(String name, String size, long sizeInBytes) {
    public String getName() {
        return name;
    }
    public String getSize() {
        return size;
    }
}
