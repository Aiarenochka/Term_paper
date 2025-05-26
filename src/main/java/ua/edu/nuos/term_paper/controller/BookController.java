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

        boolean exists = books.stream().anyMatch(b -> b.getId() == book.getId());

        if (exists) {
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
        boolean removed = books.removeIf(b -> b.getId() == id);
        if (removed) {
            bookRepository.save(books);
            redirectAttributes.addFlashAttribute("message", "Книга з ID " + id + " успішно видалена.");
        }
        else {
            redirectAttributes.addFlashAttribute("error", "Книгу з ID " + id + " не знайдено.");
        }
        return "redirect:/books";
    }

    @GetMapping("/books/edit/{id}")
    public String showEditBookForm(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Book> optionalBook = books.stream()
                .filter(b -> b.getId() == id)
                .findFirst();

        if (optionalBook.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Книга з ID " + id + " не знайдена.");
            return "redirect:/books";
        }

        model.addAttribute("book", optionalBook.get());
        return "edit_book";
    }

    @PostMapping("/books/edit/{id}")
    public String editBook(@PathVariable int id, @ModelAttribute Book updatedBook, RedirectAttributes redirectAttributes) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).getId() == id) {
                updatedBook.setId(id);
                books.set(i, updatedBook);
                bookRepository.save(books);
                redirectAttributes.addFlashAttribute("message", "Книга з ID " + id + " успішно оновлена.");
                return "redirect:/books";
            }
        }
        redirectAttributes.addFlashAttribute("error", "Книга з ID " + id + " не знайдена.");
        return "redirect:/books";
    }
    @PostMapping("/books/rate")
    public String rateBook(@RequestParam int id, @RequestParam int rating, RedirectAttributes redirectAttributes) {

        Optional<Book> optionalBook = books.stream()
                .filter(b -> b.getId() == id)
                .findFirst();

        if (optionalBook.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Книга з ID " + id + " не знайдена.");
            return "redirect:/books";
        }

        Book book = optionalBook.get();
        if (rating < 0) rating = 0;
        if (rating > 5) rating = 5;

        book.setRating(rating);
        bookRepository.save(books);

        redirectAttributes.addFlashAttribute("message", "Рейтинг книги '" + book.getTitle() + "' оновлено.");

        return "redirect:/books";
    }

    @PostMapping("/books/updateReadStatus")
    public String updateReadStatus(@RequestParam int id, @RequestParam String readStatus, RedirectAttributes redirectAttributes) {
        Optional<Book> optionalBook = books.stream()
                .filter(b -> b.getId() == id)
                .findFirst();

        if (optionalBook.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Книга з ID " + id + " не знайдена.");
            return "redirect:/books";
        }

        Book book = optionalBook.get();
        book.setReadStatus(readStatus);
        bookRepository.save(books);

        redirectAttributes.addFlashAttribute("message", "Статус прочитання книги '" + book.getTitle() + "' оновлено.");
        return "redirect:/books";
    }

}
