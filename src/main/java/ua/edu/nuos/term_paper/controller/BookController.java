package ua.edu.nuos.term_paper.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.nuos.term_paper.model.Book;
import ua.edu.nuos.term_paper.service.BookService;
import java.util.*;


@Controller
@RequestMapping("/")
public class BookController {
    private final BookService bookService;

    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
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
        model.addAttribute("books", bookService.filterAndSortBooks(genre, readStatus, minRating, sort));
        model.addAttribute("genres", bookService.getAllGenres());
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
        if (bookService.existsById(book.getId())) {
            redirectAttributes.addFlashAttribute("error", "Книга з ID " + book.getId() + " вже існує.");
            return "redirect:/books/add_book";
        }
        bookService.addBook(book);
        redirectAttributes.addFlashAttribute("message", "Книгу успішно додано: " + book.getTitle());
        return "redirect:/books";
    }

    @GetMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable int id, RedirectAttributes redirectAttributes) {
        return bookService.findById(id)
                .map(book -> {
                    bookService.deleteBook(book);
                    redirectAttributes.addFlashAttribute("message", "Книга з ID " + id + " успішно видалена.");
                    return "redirect:/books";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Книга з ID " + id + " не знайдена.");
                    return "redirect:/books";
                });
    }

    @GetMapping("/books/edit/{id}")
    public String showEditBookForm(@PathVariable int id, Model model) {
        return bookService.findById(id)
                .map(book -> {
                    model.addAttribute("book", book);
                    return "edit_book";
                })
                .orElse("redirect:/books");
    }

    @PostMapping("/books/edit/{id}")
    public String editBook(@PathVariable int id, @ModelAttribute Book updatedBook, RedirectAttributes redirectAttributes) {
        return bookService.findById(id)
                .map(existing -> {
                    bookService.updateBook(existing, updatedBook);
                    redirectAttributes.addFlashAttribute("message", "Книга з ID " + id + " оновлена.");
                    return "redirect:/books";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Книга з ID " + id + " не знайдена.");
                    return "redirect:/books";
                });
    }

    @PostMapping("/books/rate")
    public String rateBook(@RequestParam int id, @RequestParam int rating, RedirectAttributes redirectAttributes) {
        return bookService.findById(id)
                .map(book -> {
                    bookService.updateRating(book, rating);
                    redirectAttributes.addFlashAttribute("message", "Рейтинг книги '" + book.getTitle() + "' оновлено.");
                    return "redirect:/books";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Книга не знайдена.");
                    return "redirect:/books";
                });
    }

    @PostMapping("/books/updateReadStatus")
    public String updateReadStatus(@RequestParam int id, @RequestParam String readStatus, RedirectAttributes redirectAttributes) {
        return bookService.findById(id)
                .map(book -> {
                    bookService.updateReadStatus(book, readStatus);
                    redirectAttributes.addFlashAttribute("message", "Статус прочитання оновлено.");
                    return "redirect:/books";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Книга не знайдена.");
                    return "redirect:/books";
                });
    }

    @GetMapping("/books/statistics")
    public String showStatistics(Model model) {
        Map<String, Object> stats = bookService.getStatistics();
        model.addAllAttributes(stats);
        return "statistics";
    }

    @GetMapping("/authors")
    public String showAuthors(Model model) {
        model.addAttribute("authorsWithBooks", bookService.getAuthorsWithBooks());
        return "authors";
    }
}