package com.mpie.service2.service;

import com.mpie.service2.mapper.BookMapper;
import com.mpie.service2.model.Book;
import com.mpie.service2.model.BookDto;
import com.mpie.service2.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentedBookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @KafkaListener(topics = "rented-books", groupId = "book-rented-group")
    public void listen(Book bookRented) {
        log.info("Received rented book event: " + bookRented);
        saveRentedBook(bookRented);
    }

    @KafkaListener(topics = "returned-books", groupId = "book-rented-group")
    public void listenReturnedBook(Book returnedBook) {
        log.info("Received returned book event: {}", returnedBook);
        removeReturnedBook(returnedBook);
    }

    public List<BookDto> getRentedBooks(Pageable pageable) {
        return bookMapper.toDtos(bookRepository.findAll(pageable).getContent());
    }

    private void saveRentedBook(Book bookRented) {
        bookRepository.findById(bookRented.getIsbn()).ifPresentOrElse(
                book -> {
                    book.setBorrower(bookRented.getBorrower());
                    bookRepository.save(book);
                },
                () -> bookRepository.save(bookRented)
        );
    }

    public void removeReturnedBook(Book returnedBook) {
        log.info("Attempting to remove returned book with ISBN: {}", returnedBook.getIsbn());
        bookRepository.findById(returnedBook.getIsbn()).ifPresentOrElse(
                book -> {
                    log.info("Removing returned book with ISBN: {}", book.getIsbn());
                    bookRepository.delete(book);
                },
                () -> log.warn("Book with ISBN {} not found in database", returnedBook.getIsbn())
        );
    }

}
