public record FileItem(String name, String size, long sizeInBytes) {
    public String getName() {
        return name;
    }
    public String getSize() {
        return size;
    }
}
