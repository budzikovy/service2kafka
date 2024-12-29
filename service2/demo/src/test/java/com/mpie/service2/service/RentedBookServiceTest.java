package com.mpie.service2.service;

import com.mpie.service2.mapper.BookMapper;
import com.mpie.service2.model.Book;
import com.mpie.service2.model.BookDto;
import com.mpie.service2.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentedBookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private RentedBookService rentedBookService;

    @Test
    void shouldProcessRentedBook() {
        // Given
        Book rentedBook = new Book("123456", "Author1", "Title1", "Category1", "Borrower1");

        // When
        rentedBookService.listen(rentedBook);

        // Then
        verify(bookRepository, times(1)).findById("123456");
        verify(bookRepository, times(1)).save(rentedBook);
        verifyNoMoreInteractions(bookRepository);
        verifyNoInteractions(bookMapper);
    }

    @Test
    void shouldReturnRentedBooks() {
        // Given
        Pageable pageable = Pageable.unpaged();
        List<Book> books = Arrays.asList(
                new Book("123456", "Author1", "Title1", "Category1", "Borrower1"),
                new Book("789012", "Author2", "Title2", "Category2", "Borrower2")
        );
        when(bookRepository.findAll(pageable)).thenReturn(new PageImpl<>(books));

        List<BookDto> bookDtos = Arrays.asList(
                new BookDto("123456", "Author1", "Title1", "Category1", "Borrower1"),
                new BookDto("789012", "Author2", "Title2", "Category2", "Borrower2")
        );
        when(bookMapper.toDtos(books)).thenReturn(bookDtos);

        // When
        List<BookDto> result = rentedBookService.getRentedBooks(pageable);

        // Then
        assertEquals(2, result.size());
        assertEquals("Author1", result.get(0).author());
        assertEquals("Borrower2", result.get(1).borrower());

        verify(bookRepository, times(1)).findAll(pageable);
        verify(bookMapper, times(1)).toDtos(books);
    }
}