package com.mpie.service2.mapper;

import com.mpie.service2.model.Book;
import com.mpie.service2.model.BookDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookMapper {

    BookDto toDto(Book book);
    List<BookDto> toDtos(List<Book> book);
}
