package ua.edu.nuos.term_paper.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class Book implements Serializable, Comparable<Book> {
    private int id;
    private String title;
    private String author;
    private String publisher;
    private int pages;
    private int year;
    private String genre;
    private int rating;
    private String readStatus;

    @Override
    public int compareTo(Book other) {
        return Integer.compare(this.year, other.year);
    }
}


