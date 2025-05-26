package ua.edu.nuos.term_paper.repository;

import ua.edu.nuos.term_paper.model.Book;

import java.io.File;
import java.util.List;

public interface Repository<T> {
    void saveToFile(List<Book> books, File file);
    void saveToFile(List<Book> items, String fileName);
    List<Book> loadFromFile(File file);
    List<Book> loadFromFile(String fileName);
}