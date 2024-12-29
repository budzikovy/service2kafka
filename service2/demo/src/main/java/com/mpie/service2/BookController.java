package com.mpie.service2;

import com.mpie.service2.model.Book;
import com.mpie.service2.model.BookDto;
import com.mpie.service2.service.RentedBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/book")
public class BookController {

    private final RentedBookService bookService;

    @GetMapping
    List<BookDto> getRentedBooks(Pageable pageable) {
        log.info("Received get rented books request");
        return bookService.getRentedBooks(pageable);
    }

    @DeleteMapping("/{isbn}")
    public void deleteRental(@PathVariable Book isbn) {
        bookService.removeReturnedBook(isbn);
    }

}
