package ua.edu.nuos.term_paper.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.nuos.term_paper.model.Book;
import ua.edu.nuos.term_paper.repository.BookRepositoryJSONImpl;
import ua.edu.nuos.term_paper.service.BookService;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class BookController {
    private final BookService bookService;
    private final BookRepositoryJSONImpl bookRepository;
    private List<Book> books;

    @Autowired
    public BookController(BookService bookService, BookRepositoryJSONImpl bookRepository) {
        this.bookService = bookService;
        this.bookRepository = bookRepository;
        try {
            this.books = bookRepository.load();
        } catch (Exception e) {
            this.books = new ArrayList<>();
        }
    }

    @GetMapping("/")
    public String homePage() {
        return "home";
    }

    @GetMapping("/books")
    public String listBooks(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String readStatus,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) String sort,
            Model model,
            @ModelAttribute("message") String message,
            @ModelAttribute("error") String error,
            @ModelAttribute("warning") String warning
    ) {
        List<Book> filteredBooks = bookService.filterAndSortBooks(books, genre, readStatus, minRating, sort);

        Set<String> genres = books.stream()
                .map(Book::getGenre)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        model.addAttribute("books", filteredBooks);
        model.addAttribute("genres", genres);
        model.addAttribute("genre", genre);
        model.addAttribute("readStatus", readStatus);
        model.addAttribute("minRating", minRating);
        model.addAttribute("sort", sort);

        model.addAttribute("message", message);
        model.addAttribute("error", error);
        model.addAttribute("warning", warning);

        return "books";
    }

    @GetMapping("/books/add_book")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        return "add_book";
    }

    @PostMapping("/books/add_book")
    public String addBook(@ModelAttribute Book book, RedirectAttributes redirectAttributes) {
        if (books.stream().anyMatch(b -> b.getId() == book.getId())) {
            redirectAttributes.addFlashAttribute("error", "Книга з ID " + book.getId() + " вже існує. Оберіть інший ID.");
            return "redirect:/books/add_book";
        }

        int index = 0;
        while (index < books.size() && books.get(index).getId() < book.getId()) {
            index++;
        }
        books.add(index, book);
        bookRepository.save(books);
        redirectAttributes.addFlashAttribute("message", "Книгу успішно додано: " + book.getTitle());

        return "redirect:/books";
    }

    @GetMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable int id, RedirectAttributes redirectAttributes) {
        return findBookById(id, redirectAttributes)
                .map(book -> {
                    books.remove(book);
                    bookRepository.save(books);
                    redirectAttributes.addFlashAttribute("message", "Книга з ID " + id + " успішно видалена.");
                    return "redirect:/books";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Книгу з ID " + id + " не знайдено.");
                    return "redirect:/books";
                });
    }

    @GetMapping("/books/edit/{id}")
    public String showEditBookForm(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        return findBookById(id, redirectAttributes)
                .map(book -> {
                    model.addAttribute("book", book);
                    return "edit_book";
                })
                .orElse("redirect:/books");
    }

    @PostMapping("/books/edit/{id}")
    public String editBook(@PathVariable int id, @ModelAttribute Book updatedBook, RedirectAttributes redirectAttributes) {
        return findBookById(id, redirectAttributes)
                .map(book -> {
                    updatedBook.setId(id);
                    books.set(books.indexOf(book), updatedBook);
                    bookRepository.save(books);
                    redirectAttributes.addFlashAttribute("message", "Книга з ID " + id + " успішно оновлена.");
                    return "redirect:/books";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Книга з ID " + id + " не знайдена.");
                    return "redirect:/books";
                });
    }

    @PostMapping("/books/rate")
    public String rateBook(@RequestParam int id, @RequestParam int rating, RedirectAttributes redirectAttributes) {
        return findBookById(id, redirectAttributes)
                .map(book -> {
                    book.setRating(Math.max(0, Math.min(5, rating)));
                    bookRepository.save(books);
                    redirectAttributes.addFlashAttribute("message", "Рейтинг книги '" + book.getTitle() + "' оновлено.");
                    return "redirect:/books";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Книга з ID " + id + " не знайдена.");
                    return "redirect:/books";
                });
    }

    @PostMapping("/books/updateReadStatus")
    public String updateReadStatus(@RequestParam int id, @RequestParam String readStatus, RedirectAttributes redirectAttributes) {
        return findBookById(id, redirectAttributes)
                .map(book -> {
                    book.setReadStatus(readStatus);
                    bookRepository.save(books);
                    redirectAttributes.addFlashAttribute("message", "Статус прочитання книги '" + book.getTitle() + "' оновлено.");
                    return "redirect:/books";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Книга з ID " + id + " не знайдена.");
                    return "redirect:/books";
                });
    }

    private Optional<Book> findBookById(int id, RedirectAttributes redirectAttributes) {
        Optional<Book> book = books.stream()
                .filter(b -> b.getId() == id)
                .findFirst();

        if (book.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Книга з ID " + id + " не знайдена.");
        }

        return book;
    }

    @GetMapping("/books/statistics")
    public String showStatistics(Model model) {
        List<Book> allBooks = bookRepository.load();

        model.addAttribute("totalBooks", allBooks.size());

        long readCount = allBooks.stream()
                .filter(b -> "Прочитано".equals(b.getReadStatus()))
                .count();
        model.addAttribute("readBooks", readCount);
        model.addAttribute("unreadBooks", allBooks.size() - readCount);

        // Розрахунок середньої оцінки (без нулів)
        double avgRating = allBooks.stream()
                .filter(b -> b.getRating() > 0) // Ігноруємо книги з rating = 0
                .mapToInt(Book::getRating)
                .average()
                .orElse(0.0);
        model.addAttribute("avgRating", String.format("%.1f", avgRating));

        // Книги з оцінкою 5 (без змін)
        List<Book> topRated = allBooks.stream()
                .filter(b -> b.getRating() == 5)
                .collect(Collectors.toList());
        model.addAttribute("topRatedBooks", topRated);

        // Книги з оцінкою 1 (без змін)
        List<Book> lowRated = allBooks.stream()
                .filter(b -> b.getRating() == 1)
                .collect(Collectors.toList());
        model.addAttribute("lowRatedBooks", lowRated);

        return "statistics";
    }

    @GetMapping("/authors")
    public String showAuthors(Model model) {
        // Групуємо книги по авторах, ігноруючи книги без автора
        Map<String, List<Book>> authorsWithBooks = books.stream()
                .filter(book -> book.getAuthor() != null && !book.getAuthor().isEmpty())
                .collect(Collectors.groupingBy(Book::getAuthor));

        model.addAttribute("authorsWithBooks", authorsWithBooks);
        return "authors";
    }
}