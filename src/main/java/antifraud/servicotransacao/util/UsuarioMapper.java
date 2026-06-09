package antifraud.servicotransacao.util;

import antifraud.servicotransacao.dto.usuario.registro.RegistroResponseDTO;
import antifraud.servicotransacao.entity.Usuario;

public class UsuarioMapper {

    private UsuarioMapper() {}

    public static RegistroResponseDTO toResponseDTO(Usuario usuario) {
        return new RegistroResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil().toString(),
                usuario.getCriadoEm()
        );
    }
}
