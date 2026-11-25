package io.github.igorpadua.imoveis.imovel;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Imovel {

    private static final TimeBasedEpochGenerator GERADOR_DE_ID = Generators.timeBasedEpochGenerator();

    private UUID id;
    private String codigo;
    private TipoImovel tipo;
    private UsoImovel uso;
    private Endereco endereco;
    private Caracteristicas caracteristicas;
    private String matricula;
    private String inscricaoIptu;
    private Proprietarios proprietarios;
    private Situacao situacao;
    private Ocupacao ocupacao;
    private final List<Object> eventos = new ArrayList<>();

    private Imovel(String codigo, TipoImovel tipo, UsoImovel uso, Endereco endereco, Caracteristicas caracteristicas,
                   String matricula, String inscricaoIptu) {
        campoObrigatorio(tipo, "tipo");
        campoObrigatorio(uso, "uso");
        campoObrigatorio(endereco, "endereço");
        campoObrigatorio(caracteristicas, "características");
        this.codigo = Textos.obrigatorio(codigo, "código");

        this.id = GERADOR_DE_ID.generate();
        this.situacao = Situacao.RASCUNHO;
        this.ocupacao = Ocupacao.VAGO;
        this.tipo = tipo;
        this.uso = uso;
        this.endereco = endereco;
        this.caracteristicas = caracteristicas;
        this.matricula = Textos.opcional(matricula);
        this.inscricaoIptu = Textos.opcional(inscricaoIptu);

    }

    public static Imovel cadastrar(String codigo, TipoImovel tipo, UsoImovel uso, Endereco endereco, Caracteristicas caracteristicas,
                                   String matricula, String inscricaoIptu) {
        Imovel imovel = new Imovel(codigo, tipo, uso, endereco, caracteristicas, matricula, inscricaoIptu);
        imovel.eventos.add(new ImovelCadastrado(imovel.id));
        return imovel;
    }

    public void definirProprietarios(Proprietarios proprietarios) {
        campoObrigatorio(proprietarios, "proprietários");
        this.proprietarios = proprietarios;
    }

    public void ativar() {
        if (this.situacao == Situacao.ATIVO) {
            throw new RegraDeDominioException("O imóvel já está ativo.");
        }
        if (this.proprietarios == null) {
            throw new RegraDeDominioException("O imóvel precisa ter proprietário definido para ser ativado.");
        }
        this.situacao = Situacao.ATIVO;
    }

    public void inativar() {
        if (this.situacao == Situacao.INATIVO) {
            throw new RegraDeDominioException("O imóvel já está inativo.");
        }
        this.situacao = Situacao.INATIVO;
    }

    private void campoObrigatorio(Object valor, String campo) {
        if (valor == null) {
            throw new RegraDeDominioException("O campo " + campo + " é obrigatório.");
        }
    }

    public UUID id() {
        return id;
    }

    public Situacao situacao() {
        return situacao;
    }

    public Ocupacao ocupacao() {
        return ocupacao;
    }

    public Proprietarios proprietarios() {
        return proprietarios;
    }

    public String codigo() {
        return codigo;
    }

    public TipoImovel tipo() {
        return tipo;
    }

    public UsoImovel uso() {
        return uso;
    }

    public Endereco endereco() {
        return endereco;
    }

    public Caracteristicas caracteristicas() {
        return caracteristicas;
    }

    public String matricula() {
        return matricula;
    }

    public String inscricaoIptu() {
        return inscricaoIptu;
    }

    public List<Object> eventos() {
        return List.copyOf(eventos);
    }

}
