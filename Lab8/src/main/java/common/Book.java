package common;

import java.io.Serializable;

/**
 * Model đại diện cho một cuốn sách trong thư viện.
 * Cần implements Serializable để truyền object qua mạng giữa Client và Server thông qua Java RMI.
 */
public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String author;
    private boolean available;
    private String borrowedBy; // Tên người đang mượn sách (null nếu sách chưa bị mượn)

    public Book() {
    }

    public Book(int id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.available = true;
        this.borrowedBy = null;
    }

    public Book(int id, String title, String author, boolean available, String borrowedBy) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.available = available;
        this.borrowedBy = borrowedBy;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getBorrowedBy() {
        return borrowedBy;
    }

    public void setBorrowedBy(String borrowedBy) {
        this.borrowedBy = borrowedBy;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s - %s (%s%s)",
                id,
                title,
                author,
                available ? "Available" : "Borrowed",
                (!available && borrowedBy != null) ? " by " + borrowedBy : "");
    }
}

