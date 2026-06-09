package antifraud.servicotransacao.util;

import org.springframework.data.domain.Page;

public class PaginaResponseMapper {

    private PaginaResponseMapper() {}

    public static <T> PaginaResponseDTO<T> fromPage(Page<T> page) {
        return new PaginaResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize()
        );
    }
}
