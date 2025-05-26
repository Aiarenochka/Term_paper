package ua.edu.nuos.term_paper.service;

import org.springframework.stereotype.Service;
import ua.edu.nuos.term_paper.model.Book;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BookService {

    public List<Book> filterAndSortBooks(List<Book> books,
                                         String genre,
                                         String readStatus,
                                         Integer minRating,
                                         String sortBy) {
        Stream<Book> stream = books.stream();

        if (genre != null && !genre.isEmpty()) {
            stream = stream.filter(book -> genre.equalsIgnoreCase(book.getGenre()));
        }

        if (readStatus != null && !readStatus.isEmpty()) {
            stream = stream.filter(book -> readStatus.equalsIgnoreCase(book.getReadStatus()));
        }

        if (minRating != null) {
            stream = stream.filter(book -> book.getRating() >= minRating);
        }

        if (sortBy != null) {
            stream = switch (sortBy) {
                case "title" -> stream.sorted(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));
                case "year" -> stream.sorted(Comparator.comparingInt(Book::getYear));
                case "rating" -> stream.sorted(Comparator.comparingDouble(Book::getRating).reversed());
                default -> stream;
            };
        }

        return stream.collect(Collectors.toList());
    }
}
