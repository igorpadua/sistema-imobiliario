package io.github.igorpadua.imoveis.imovel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.github.igorpadua.imoveis.imovel.EnderecoBuilder.umEndereco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnderecoTest {

    @ParameterizedTest(name = "latitude {0} é aceita")
    @CsvSource({"-90", "90", "0"})
    void aceitaLatitudeDentroDaFaixa(double latitude) {
        assertThat(umEndereco().comCoordenadas(latitude, 0d).build().latitude()).isEqualTo(latitude);
    }

    @ParameterizedTest(name = "latitude {0} é recusada")
    @CsvSource({"-90.000001", "90.000001"})
    void recusaLatitudeForaDaFaixa(double latitude) {
        assertThatThrownBy(() -> umEndereco().comCoordenadas(latitude, 0d).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "latitude {0} é recusada")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void recusaLatitudeNanEInfinito(double latitude) {
        assertThatThrownBy(() -> umEndereco().comCoordenadas(latitude, 0d).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "longitude {0} é aceita")
    @CsvSource({"-180", "180", "0"})
    void aceitaLongitudeDentroDaFaixa(double longitude) {
        assertThat(umEndereco().comCoordenadas(0d, longitude).build().longitude()).isEqualTo(longitude);
    }

    @ParameterizedTest(name = "longitude {0} é recusada")
    @CsvSource({"-180.000001", "180.000001"})
    void recusaLongitudeForaDaFaixa(double longitude) {
        assertThatThrownBy(() -> umEndereco().comCoordenadas(0d, longitude).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "longitude {0} é recusada")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void recusaLongitudeNanEInfinito(double longitude) {
        assertThatThrownBy(() -> umEndereco().comCoordenadas(0d, longitude).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void aceitaEnderecoSemCoordenadas() {
        Endereco endereco = umEndereco().semCoordenadas().build();
        assertThat(endereco.latitude()).isNull();
        assertThat(endereco.longitude()).isNull();
    }

    @Test
    void recusaLatitudeSemLongitude() {
        assertThatThrownBy(() -> umEndereco().comCoordenadas(0d, null).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaLongitudeSemLatitude() {
        assertThatThrownBy(() -> umEndereco().comCoordenadas(null, 0d).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "logradouro \"{0}\" é recusado")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t"})
    void recusaLogradouroSemValor(String logradouro) {
        assertThatThrownBy(() -> umEndereco().comLogradouro(logradouro).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "bairro \"{0}\" é recusado")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t"})
    void recusaBairroSemValor(String bairro) {
        assertThatThrownBy(() -> umEndereco().comBairro(bairro).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "cidade \"{0}\" é recusada")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t"})
    void recusaCidadeSemValor(String cidade) {
        assertThatThrownBy(() -> umEndereco().comCidade(cidade).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "número \"{0}\" é recusado")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t"})
    void recusaNumeroSemValor(String numero) {
        assertThatThrownBy(() -> umEndereco().comNumero(numero).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void aceitaNumeroSN() {
        assertThat(umEndereco().comNumero("S/N").build().numero()).isEqualTo("S/N");
    }

    @ParameterizedTest(name = "complemento \"{0}\" vira null")
    @EmptySource
    @ValueSource(strings = {" ", "  ", "\t"})
    void transformaComplementoVazioEmNull(String complemento) {
        assertThat(umEndereco().comComplemento(complemento).build().complemento()).isNull();
    }

    @Test
    void aceitaComplementoNull() {
        assertThat(umEndereco().semComplemento().build().complemento()).isNull();
    }

    @ParameterizedTest(name = "logradouro \"{0}\" é guardado como \"{1}\"")
    @CsvSource({"' Rua das Flores ', Rua das Flores", "' Rua do teste', Rua do teste", "'Rua outra ', Rua outra"})
    void removeEspacosPontasLogradouro(String logradouro, String logradouroEsperado) {
        assertThat(umEndereco().comLogradouro(logradouro).build().logradouro()).isEqualTo(logradouroEsperado);
    }

    @ParameterizedTest(name = "número \"{0}\" é guardado como \"{1}\"")
    @CsvSource({"' 323 ', 323", "' 323', 323", "'323 ', 323"})
    void removeEspacosPontasNumero(String numero, String numeroEsperado) {
        assertThat(umEndereco().comNumero(numero).build().numero()).isEqualTo(numeroEsperado);
    }

    @ParameterizedTest(name = "complemento \"{0}\" é guardado como \"{1}\"")
    @CsvSource({"' Ao lado ', Ao lado", "' Ao lado', Ao lado", "'Ao lado ', Ao lado"})
    void removeEspacosPontasComplemento(String complemento, String complementoEsperado) {
        assertThat(umEndereco().comComplemento(complemento).build().complemento()).isEqualTo(complementoEsperado);
    }

    @ParameterizedTest(name = "bairro \"{0}\" é guardado como \"{1}\"")
    @CsvSource({"' Setor X ', Setor X", "' Setor X', Setor X", "' Setor X  ', Setor X"})
    void removeEspacosPontasBairro(String bairro, String bairroEsperado) {
        assertThat(umEndereco().comBairro(bairro).build().bairro()).isEqualTo(bairroEsperado);
    }

    @ParameterizedTest(name = "cidade \"{0}\" é guardada como \"{1}\"")
    @CsvSource({"' São Paulo ', São Paulo", "' São Paulo', São Paulo", "'São Paulo ', São Paulo"})
    void removeEspacosPontasCidade(String cidade, String cidadeEsperada) {
        assertThat(umEndereco().comCidade(cidade).build().cidade()).isEqualTo(cidadeEsperada);
    }

    @Test
    void recusaUfNula() {
        assertThatThrownBy(() -> umEndereco().semUf().build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "CEP {0} é aceito")
    @CsvSource({"64207012", "41301660", "41611020"})
    void aceitaCep(String cep) {
        assertThat(umEndereco().comCep(cep).build().cep()).isEqualTo(cep);
    }

    @ParameterizedTest(name = "CEP \"{0}\" com hífen é guardado como \"{1}\"")
    @CsvSource({"81925-186, 81925186", "58417-607, 58417607", "83411-202, 83411202"})
    void aceitaCepComHifen(String cep, String cepEsperado) {
        assertThat(umEndereco().comCep(cep).build().cep()).isEqualTo(cepEsperado);
    }

    @ParameterizedTest(name = "CEP \"{0}\" com espaços nas pontas é guardado como \"{1}\"")
    @CsvSource({"'81925186 ', 81925186", "' 58417-607 ', 58417607", "' 83411-202', 83411202"})
    void aceitaCepComEspacosPonta(String cep, String cepEsperado) {
        assertThat(umEndereco().comCep(cep).build().cep()).isEqualTo(cepEsperado);
    }

    @ParameterizedTest(name = "CEP \"{0}\" é recusado")
    @NullAndEmptySource
    void recusaCepNullVazio(String cep) {
        assertThatThrownBy(() -> umEndereco().comCep(cep).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "CEP {0} sem 8 dígitos é recusado")
    @CsvSource({"6070121", "413016603", "416105"})
    void recusaCepDiferenteOitoDigitos(String cep) {
        assertThatThrownBy(() -> umEndereco().comCep(cep).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "CEP {0} com letras é recusado")
    @CsvSource({"6420701a", "4130160c", "4161102d"})
    void recusaCepComLetras(String cep) {
        assertThatThrownBy(() -> umEndereco().comCep(cep).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "CEP {0} com hífen fora da posição é recusado")
    @CsvSource({"58417607-", "58417607--", "584176-07"})
    void recusaCepHifenForaLugar(String cep) {
        assertThatThrownBy(() -> umEndereco().comCep(cep).build())
                .isInstanceOf(RegraDeDominioException.class);
    }
}