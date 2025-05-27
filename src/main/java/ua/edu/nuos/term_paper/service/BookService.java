package ua.edu.nuos.term_paper.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.edu.nuos.term_paper.model.Book;
import ua.edu.nuos.term_paper.repository.BookRepositoryJSONImpl;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BookService {

    private final BookRepositoryJSONImpl bookRepository;
    private List<Book> books;

    @Autowired
    public BookService(BookRepositoryJSONImpl bookRepository) {
        this.bookRepository = bookRepository;
        try {
            this.books = bookRepository.load();
        } catch (Exception e) {
            this.books = new ArrayList<>();
        }
    }

    public void addBook(Book book) {
        int index = 0;
        while (index < books.size() && books.get(index).getId() < book.getId()) {
            index++;
        }
        books.add(index, book);
        saveBooks();
    }

    public boolean existsById(int id) {
        return books.stream().anyMatch(b -> b.getId() == id);
    }

    public Optional<Book> findById(int id) {
        return books.stream().filter(b -> b.getId() == id).findFirst();
    }

    public void deleteBook(Book book) {
        books.remove(book);
        saveBooks();
    }

    public void updateBook(Book existingBook, Book updatedBook) {
        updatedBook.setId(existingBook.getId());
        books.set(books.indexOf(existingBook), updatedBook);
        saveBooks();
    }

    public void updateRating(Book book, int rating) {
        book.setRating(Math.max(0, Math.min(5, rating)));
        saveBooks();
    }

    public void updateReadStatus(Book book, String readStatus) {
        book.setReadStatus(readStatus);
        saveBooks();
    }

    public List<Book> filterAndSortBooks(String genre, String readStatus, Integer minRating, String sortBy) {
        Stream<Book> stream = books.stream();

        if (genre != null && !genre.isEmpty()) {
            stream = stream.filter(book -> genre.equalsIgnoreCase(book.getGenre()));
        }

        if (readStatus != null && !readStatus.isEmpty()) {
            stream = stream.filter(book -> readStatus.equals(book.getReadStatus()));
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

    public Set<String> getAllGenres() {
        return books.stream()
                .map(Book::getGenre)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Map<String, List<Book>> getAuthorsWithBooks() {
        return books.stream()
                .filter(book -> book.getAuthor() != null && !book.getAuthor().isEmpty())
                .collect(Collectors.groupingBy(Book::getAuthor));
    }

    public Map<String, Object> getStatistics() {
        List<Book> allBooks = bookRepository.load();
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalBooks", allBooks.size());

        long readCount = allBooks.stream()
                .filter(b -> "Прочитано".equals(b.getReadStatus()))
                .count();

        stats.put("readBooks", readCount);
        stats.put("unreadBooks", allBooks.size() - readCount);

        double avgRating = allBooks.stream()
                .filter(b -> b.getRating() > 0)
                .mapToInt(Book::getRating)
                .average()
                .orElse(0.0);
        stats.put("avgRating", String.format("%.1f", avgRating));

        stats.put("topRatedBooks", allBooks.stream().filter(b -> b.getRating() == 5).toList());
        stats.put("lowRatedBooks", allBooks.stream().filter(b -> b.getRating() == 1).toList());

        return stats;
    }

    public void saveBooks() {
        bookRepository.save(books);
    }
}