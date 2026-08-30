package antifraud.servicotransacao.repository;

import antifraud.servicotransacao.entity.Transacao;
import antifraud.servicotransacao.enums.StatusTransacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    Page<Transacao> findByContaId(UUID contaId, Pageable pageable);

    List<Transacao> findByContaIdAndStatus(UUID contaId, StatusTransacao status);
}
