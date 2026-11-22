package io.github.igorpadua.imoveis.imovel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static io.github.igorpadua.imoveis.imovel.ImovelBuilder.umImovel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImovelTest {

    @Test
    void imovelNasceComIdPreenchidoV7() {
        assertThat(umImovel().build().id().version()).isEqualTo(7);
    }

    @Test
    void doisImoveisCriadosEmSeguidaPossuemIdsDiferentes() {
        Imovel primeiroImovel = umImovel().build();
        Imovel segundoImovel = umImovel().build();

        assertThat(primeiroImovel.id()).isNotEqualTo(segundoImovel.id());
    }

    @Test
    void imovelNovoNasceComSituacaoRascunho() {
        assertThat(umImovel().build().situacao()).isEqualTo(Situacao.RASCUNHO);
    }

    @Test
    void imovelNovoNasceComOcupacaoVago() {
        assertThat(umImovel().build().ocupacao()).isEqualTo(Ocupacao.VAGO);
    }

    @Test
    void imovelNovoNasceSemProprietarios() {
        assertThat(umImovel().build().proprietarios()).isNull();
    }

    @Test
    void imovelNovoSalvaCodigo() {
        assertThat(umImovel().comCodigo("TESTE").build().codigo()).isEqualTo("TESTE");
    }

    @Test
    void imovelNovoSalvaTipo() {
        assertThat(umImovel().comTipo(TipoImovel.CASA).build().tipo()).isEqualTo(TipoImovel.CASA);
    }

    @Test
    void imovelNovoSalvaUso() {
        assertThat(umImovel().comUso(UsoImovel.COMERCIAL).build().uso()).isEqualTo(UsoImovel.COMERCIAL);
    }

    @Test
    void imovelNovoSalvaEndereco() {
        Endereco endereco = EnderecoBuilder.umEndereco().comCidade("Goiânia").comComplemento("Rua nova").build();
        assertThat(umImovel().comEndereco(endereco).build().endereco()).isEqualTo(endereco);
    }

    @Test
    void imovelNovoSalvaCaracteristicas() {
        Caracteristicas caracteristica = CaracteristicasBuilder.umaCaracteristica().comQuartos(3).comSuites(1).build();
        assertThat(umImovel().comCaracteristicas(caracteristica).build().caracteristicas()).isEqualTo(caracteristica);
    }

    @ParameterizedTest(name = "Recusa código \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t"})
    void recusaCodigo(String codigo) {
        assertThatThrownBy(() -> umImovel().comCodigo(codigo).build())
            .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaTipoNulo() {
        assertThatThrownBy(() -> umImovel().comTipo(null).build())
            .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaUsoNulo() {
        assertThatThrownBy(() -> umImovel().comUso(null).build())
            .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaEnderecoNulo() {
        assertThatThrownBy(() -> umImovel().comEndereco(null).build())
            .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaCaracteristicasNulas() {
        assertThatThrownBy(() -> umImovel().comCaracteristicas(null).build())
            .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void aceitaMatriculaNula() {
        assertThat(umImovel().comMatricula(null).build().matricula()).isNull();
    }

    @ParameterizedTest(name = "Matrícula \"{0}\" é guardada como nula")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t"})
    void transformaMatriculaVaziaEmNula(String matricula) {
        assertThat(umImovel().comMatricula(matricula).build().matricula()).isNull();
    }

    @ParameterizedTest(name = "Matrícula \"{0}\" é guardada como \"{1}\"")
    @CsvSource({"' teste ', teste", "' teste 3', teste 3", "'outro ', outro"})
    void removeEspacosPontasMatricula(String matricula, String matriculaEsperada) {
        assertThat(umImovel().comMatricula(matricula).build().matricula()).isEqualTo(matriculaEsperada);
    }

    @Test
    void aceitaInscricaoIptuNula() {
        assertThat(umImovel().comInscricaoIptu(null).build().inscricaoIptu()).isNull();
    }

    @ParameterizedTest(name = "Inscrição de IPTU \"{0}\" é guardada como nula")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t"})
    void transformaInscricaoIptuVaziaEmNula(String inscricaoIptu) {
        assertThat(umImovel().comInscricaoIptu(inscricaoIptu).build().inscricaoIptu()).isNull();
    }

    @ParameterizedTest(name = "Inscrição de IPTU \"{0}\" é guardada como \"{1}\"")
    @CsvSource({"' teste ', teste", "' teste 3', teste 3", "'outro ', outro"})
    void removeEspacosPontasInscricaoIptu(String inscricaoIptu, String inscricaoIptuEsperada) {
        assertThat(umImovel().comInscricaoIptu(inscricaoIptu).build().inscricaoIptu()).isEqualTo(inscricaoIptuEsperada);
    }

    @Test
    void fabricaRegistraUmImovelCadastradoComIdDoImovel() {
        Imovel imovel = umImovel().build();
        assertThat(imovel.eventos()).containsExactly(new ImovelCadastrado(imovel.id()));
    }

    @Test
    void naoEPossivelAlterarAListaDeEventos() {
        Imovel imovel = umImovel().build();
        assertThatThrownBy(() -> imovel.eventos().add(new ImovelCadastrado(imovel.id())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void numImovelSemProprietariosGravaAListaInformada() {
        Imovel imovel = umImovel().build();
        assertThat(imovel.proprietarios()).isNull();
        Proprietarios proprietarios = new Proprietarios(List.of(new Proprietario(UUID.randomUUID(), new BigDecimal("100"))));
        imovel.definirProprietarios(proprietarios);
        assertThat(imovel.proprietarios().proprietarios()).containsExactlyElementsOf(proprietarios.proprietarios());
    }

    @Test
    void numImovelComProprietariosTrocaAListaInteiraDeProprietarios() {
        Proprietarios proprietarios = new Proprietarios(List.of(new Proprietario(UUID.randomUUID(), new BigDecimal("100"))));
        Imovel imovel = umImovel().comProprietarios(proprietarios).build();
        assertThat(imovel.proprietarios().proprietarios()).containsExactlyElementsOf(proprietarios.proprietarios());
        proprietarios = new Proprietarios(List.of(new Proprietario(UUID.randomUUID(), new BigDecimal("50")), new Proprietario(UUID.randomUUID(), new BigDecimal("50"))));
        imovel.definirProprietarios(proprietarios);
        assertThat(imovel.proprietarios().proprietarios()).containsExactlyElementsOf(proprietarios.proprietarios());
    }

    @Test
    void numImovelComProprietariosRecusaListaNula() {
        Proprietarios proprietarios = new Proprietarios(List.of(new Proprietario(UUID.randomUUID(), new BigDecimal("100"))));
        Imovel imovel = umImovel().comProprietarios(proprietarios).build();
        assertThatThrownBy(() -> imovel.definirProprietarios(null))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Código \"{0}\" é guardado como \"{1}\"")
    @CsvSource({"' teste ', teste", "' teste 3', teste 3", "'outro ', outro"})
    void removeEspacosPontasCodigo(String codigo, String codigoEsperado) {
        assertThat(umImovel().comCodigo(codigo).build().codigo()).isEqualTo(codigoEsperado);
    }

}
