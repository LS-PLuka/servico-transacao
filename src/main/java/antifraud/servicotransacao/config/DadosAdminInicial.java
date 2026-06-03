package antifraud.servicotransacao.config;

import antifraud.servicotransacao.entity.Usuario;
import antifraud.servicotransacao.enums.PerfilUsuario;
import antifraud.servicotransacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
//essa classe foi criada para implementaçao de um Usuario ADMIN inicial
//esse ADMIN é carregado automaticamente no banco de dados quando a aplicação é iniciada, caso ele ainda não exista
//em caso de duvida, recomendo que leia a documentaçao para melhor entendimento!!!
public class DadosAdminInicial implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@antifraude.com}")
    private String adminEmail;

    @Value("${admin.senha:admin123456}")
    private String adminSenha;

    @Value("${admin.nome:Administrador}")
    private String adminNome;

    @Override
    public void run(String... args) {
        //se ja existir um usuario com o email do admin ele apenas ignora
        if (usuarioRepository.existsByEmail(adminEmail)) return;

        //caso nao exista ele cria um novo usuario com o perfil ADMIN
        Usuario admin = Usuario.builder()
                .nome(adminNome)
                .email(adminEmail)
                .senha(passwordEncoder.encode(adminSenha))
                .perfil(PerfilUsuario.ADMIN)
                .criadoEm(LocalDateTime.now())
                .build();

        usuarioRepository.save(admin);
        log.info("Usuário ADMIN inicial criado: {}", adminEmail);
    }
}
