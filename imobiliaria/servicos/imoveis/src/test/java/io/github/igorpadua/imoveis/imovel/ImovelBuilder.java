package io.github.igorpadua.imoveis.imovel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

class ImovelBuilder {

    private String codigo = "QUALQUER";
    private TipoImovel tipoImovel = TipoImovel.APARTAMENTO;
    private UsoImovel usoImovel = UsoImovel.RESIDENCIAL;
    private Endereco endereco = EnderecoBuilder.umEndereco().build();
    private Caracteristicas caracteristicas = CaracteristicasBuilder.umaCaracteristica().build();
    private String matricula = "QUALQUER";
    private String inscricaoIptu = "QUALQUER";
    private Proprietarios proprietarios = null;

    static ImovelBuilder umImovel() {
        return new ImovelBuilder();
    }

    ImovelBuilder comCodigo(String codigo) {
        this.codigo = codigo;
        return this;
    }

    ImovelBuilder comTipo(TipoImovel tipoImovel) {
        this.tipoImovel = tipoImovel;
        return this;
    }

    ImovelBuilder comUso(UsoImovel usoImovel) {
        this.usoImovel = usoImovel;
        return this;
    }

    ImovelBuilder comEndereco(Endereco endereco) {
        this.endereco = endereco;
        return this;
    }

    ImovelBuilder comCaracteristicas(Caracteristicas caracteristicas) {
        this.caracteristicas = caracteristicas;
        return this;
    }

    ImovelBuilder comMatricula(String matricula) {
        this.matricula = matricula;
        return this;
    }

    ImovelBuilder comInscricaoIptu(String inscricaoIptu) {
        this.inscricaoIptu = inscricaoIptu;
        return this;
    }

    ImovelBuilder comProprietarios(Proprietarios proprietarios) {
        this.proprietarios = proprietarios;
        return this;
    }

    ImovelBuilder comUmProprietario() {
        return comProprietarios(new Proprietarios(List.of(new Proprietario(UUID.randomUUID(), new BigDecimal("100")))));
    }

    Imovel build() {
        Imovel imovel = Imovel.cadastrar(
                codigo,
                tipoImovel,
                usoImovel,
                endereco,
                caracteristicas,
                matricula,
                inscricaoIptu);

        if (proprietarios != null) {
            imovel.definirProprietarios(proprietarios);
        }

        return imovel;
    }

}
