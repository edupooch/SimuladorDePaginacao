package controle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import modelo.Frame;
import modelo.Pagina;

/**
 * CLASSE REPRESENTANDO A MMU - UNIDADE DE GERENCIAMENTO DE MEM�RIA: Respons�vel
 * por manter a tabela de p�ginas virtuais e o mapeamento entre p�ginas e frames
 * 
 * @author edupooch
 *
 */
public class MMU {

	/**
	 * 1KB EM BYTES = 1024, n�mero necess�rio para calcular a posi��o do frame
	 * em bytes para a instru��o MOV REG
	 */
	public final static int KB_EM_BYTES = 1024;

	/**
	 * MEM�RIA PRINCIPAL (MP) - Possui 64KF
	 * 
	 * A mem�ria principal recebe um objeto do tipo FRAME, que possui o conte�do
	 * de uma p�gina
	 */
	static Frame[] memoriaPrincipal;


	/**
	 * MEM�RIA VIRTUAL (MV)
	 * 
	 * Cada posi��o cont�m um objeto p�gina, que possui uma string que
	 * referencia o caminho para um arquivo que tem o conte�do da p�gina
	 * 
	 */
	static Pagina[] memoriaVirtual;

	/**
	 * TABELA DE P�GINAS - Cont�m as informa��es de mapeamento entre p�ginas e
	 * frames, tem o tamanho igual ao do n�mero de p�ginas e 3 colunas com os
	 * valores dos ponteiros.
	 * 
	 * O �ndice do vetor representa o �ndice da mem�ria virtual, e o valor
	 * dentro do vetor desse �ndice representa:
	 * 
	 * �NDICE 0 - N�MERO DA POSI��O DA MOLDURA NA MEM�RIA PRINCIPAL;
	 * 
	 * INDICE 1 - BIT DE PRESENTE/AUSENTE;
	 * 
	 * INDICE 2 - BIT MODIFICADA.
	 * 
	 * 
	 * BITS DE REFER�NCIA - MATRIZ SEPARADA DA TABELA DE P�GINAS
	 * 
	 * PROTE��O - ESCRITA/ LEITURA N�O INCLUSO NESSE SISTEMA
	 * 
	 * 
	 */
	public static Integer[][] tabelaDePaginas;
	public final static int QUANTIDADE_DE_COLUNAS_DA_TABELA = 3;
	public final static int NUMERO_MOLDURA = 0;
	public final static int BIT_PRESENTE = 1;
	public final static int BIT_MODIFICADA = 2;

	/**
	 * Matriz de bits R - para fazer o algoritmo aging. Esta matriz de TAMANHO
	 * DA MEMORIA PRINCIPAL X 8 BITS QUE SER�O GUARDADOS, possui as informa��es
	 * de quais as posi��es da mem�ria principal que foram referenciadas nos
	 * �ltimos 8 ciclos, a com o menor valor � retirada segundo a l�gica do
	 * algoritmo aging.
	 */
	static int[][] bitsReferencia;
	private final static int QUANTIDADE_BITS_REFERENCIA = 8;
	
	
	
	private static int posicaoSolicitada;

	/**
	 * Vari�vel para definir o tipo de acesso que o processador solicitou
	 */
	public final static int GRAVAR_TEMPO = 0;

	/**
	 * Tamanho da p�gina
	 */
	private static int tamanhoPagina;

	public MMU(int tamanhoDaMemoriaVirtual, int tamanhoDaMemoriaFisica, int tamanhoDaPagina) {

		memoriaVirtual = new Pagina[tamanhoDaMemoriaVirtual];
		tabelaDePaginas = new Integer[tamanhoDaMemoriaVirtual][QUANTIDADE_DE_COLUNAS_DA_TABELA];

		memoriaPrincipal = new Frame[tamanhoDaMemoriaFisica];
		bitsReferencia = new int[tamanhoDaMemoriaFisica][QUANTIDADE_BITS_REFERENCIA];
		
		tamanhoPagina = tamanhoDaPagina;

		for (int i = 0; i < tamanhoDaMemoriaVirtual; i++) {
			tabelaDePaginas[i][BIT_PRESENTE] = 0;
			tabelaDePaginas[i][BIT_MODIFICADA] = 0;
			tabelaDePaginas[i][NUMERO_MOLDURA] = -1;

		}
	}

	public static boolean mapeamento(int posicao, int tipoAcesso) {
		long inicio = System.currentTimeMillis();
		boolean hit;
		realocaBitsR();
		posicaoSolicitada = posicao;
		
		System.out.println("Solicita��o de acesso (l�gico) do processador: MOV REG, "
				+ posicaoSolicitada * tamanhoPagina * KB_EM_BYTES + " (POSI��O " + posicaoSolicitada +")");
		
		Pagina pagina = memoriaVirtual[posicaoSolicitada];

		if (tabelaDePaginas[posicaoSolicitada][BIT_PRESENTE] == 1) {
			hit = true;
			System.out.println("P�gina presente na mem�ria principal na posi��o "
					+ tabelaDePaginas[posicaoSolicitada][NUMERO_MOLDURA]);
			bitsReferencia[tabelaDePaginas[posicaoSolicitada][NUMERO_MOLDURA]][0] = 1;

		} else {
			hit = false;
			// A p�gina n�o est� carregada na mem�ria principal e precisa ser
			// carregada
			System.out.println("P�gina ausente na mem�ria principal!");
			carregaPaginaNaMP(pagina);

		}
		int posicaoFisica = tabelaDePaginas[posicaoSolicitada][NUMERO_MOLDURA];

		switch (tipoAcesso) {
		case GRAVAR_TEMPO:
			long tempo = System.currentTimeMillis() - inicio;
			memoriaPrincipal[posicaoFisica].setConteudo(tempo);
			tabelaDePaginas[posicaoSolicitada][BIT_MODIFICADA] = 1;
			break;

		}

		System.out.println("Solicita��o de acesso (f�sico) do processador: MOV REG, "
				+ posicaoFisica * tamanhoPagina * KB_EM_BYTES + " (POSI��O " + posicaoFisica + ")");

		return hit;
	}

	/**
	 * Procura um frame dispon�vel na mem�ria principal e salva a p�gina naquele
	 * frame, como ela � referenciada nessa instru��o, o m�todo grava 1 no array
	 * de bits R na primeira posi��o do frame disponivel
	 * 
	 * @param pagina
	 */
	private static void carregaPaginaNaMP(Pagina pagina) {

		int frameDisponivel = procuraFrameDisponivel();

		// Passa o conte�do do disco para o frame
		try {
			FileInputStream arquivoLeitura = new FileInputStream(pagina.getArquivoConteudo());
			ObjectInputStream objLeitura = new ObjectInputStream(arquivoLeitura);

			memoriaPrincipal[frameDisponivel] = new Frame();
			memoriaPrincipal[frameDisponivel].setConteudo(objLeitura.readObject());

			arquivoLeitura.close();
			objLeitura.close();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}

		// Salva a informa��o do numero da moldura na tabela de p�ginas
		tabelaDePaginas[posicaoSolicitada][NUMERO_MOLDURA] = frameDisponivel;
		// Coloca na tabela que a pagina est� presente na mem�ria principal
		tabelaDePaginas[posicaoSolicitada][BIT_PRESENTE] = 1;

		System.out.println("P�gina carregada na posi��o " + frameDisponivel);

		// Atualiza a matriz de bits de referencia, colocando 1 para a
		// solicitada
		bitsReferencia[frameDisponivel][0] = 1;

		// Print da matriz de bits ap�s a realoca��o e referencia da pagina nova
		System.out.println("\nBits de refer�ncia:");
		for (int i = 0; i < bitsReferencia.length; i++) {
			System.out.print("Posi��o " + i + ": ");
			for (int j = 0; j < QUANTIDADE_BITS_REFERENCIA; j++)
				System.out.print(bitsReferencia[i][j] + " ");
			System.out.println("");
		}

	}

	/**
	 * Realoca os bits R de todas as p�ginas da memoria fisica
	 */
	private static void realocaBitsR() {
		// Realocar bits pra direita
		for (int i = 0; i < bitsReferencia.length; i++) {
			for (int j = QUANTIDADE_BITS_REFERENCIA - 1; j > 0; j--)
				bitsReferencia[i][j] = bitsReferencia[i][j - 1];
			// Primeira posi��o � zerada, ser� marcado um para os referenciados
			// no ciclo
			bitsReferencia[i][0] = 0;
		}
	}

	private static int procuraFrameDisponivel() {
		System.out.println("Procurando frame dispon�vel...");
		for (int i = 0; i < memoriaPrincipal.length; i++) {
			if (memoriaPrincipal[i] == null) {
				System.out.println("Frame " + i + " dispon�vel!");
				return i;
				// Caso haja alguma p�gina dispon�vel o m�todo para a execu��o
				// aqui e retorna o n�mero desse frame, se n�o a execu��o
				// continua para liberar um frame
			}
		}
		System.out.println("N�o existem frames dispon�veis!");
		int frameLiberado = liberaUmFrame();
		return frameLiberado;

	}

	private static int liberaUmFrame() {
		System.out.println("Verificando o melhor frame para ser liberado...");
		// Criando um array com 8 valores de refer�ncia para verificar depois
		// qual foi a posi��o menos referenciada
		int[] valores = new int[memoriaPrincipal.length];
		System.out.println("Matriz dos bits de refer�ncia:");
		for (int i = 0; i < bitsReferencia.length; i++) {
			System.out.print("Posi��o " + i + ": ");
			for (int j = 0; j < QUANTIDADE_BITS_REFERENCIA; j++) {
				// Valor salvo no array valores corresponde a uma sequ�ncia de
				// bin�rios, multplica-se pelo 10^j para se obter um n�mero de
				// at� 8 d�gitos ex 10001010, representando as refer�ncias nos
				// �ltimos ciclos
				valores[i] += bitsReferencia[i][j] * Math.pow(10, 7 - j);
				System.out.print(bitsReferencia[i][j] + " ");
			}
			System.out.println("");
		}

		// Pegar a posi��o com o menor valor dentro do array valores para ver
		// qual � a posi��o que dever� ser liberada
		int minimo = valores[0];
		int posicaoLiberada = 0;
		for (int i = 1; i < valores.length; i++) {
			if (valores[i] < minimo) {
				minimo = valores[i];
				posicaoLiberada = i;
			}
		}
		System.out.println("Posi��o " + posicaoLiberada + " tem o menor valor, portanto ser� liberada");

		// Verificando qual foi a p�gina virtual que foi removida da mem�ria
		// fisica
		int posicaoPaginaRemovida = -1;
		for (int i = 0; i < tabelaDePaginas.length; i++) {
			if (tabelaDePaginas[i][NUMERO_MOLDURA] == posicaoLiberada) {
				posicaoPaginaRemovida = i;
			}
		}

		System.out.println("Verificando se a p�gina foi modificada...");
		System.out.println("Bit de modifica��o igual a " + tabelaDePaginas[posicaoPaginaRemovida][BIT_MODIFICADA]);

		// Caso o bit sujo seja 1, salva-se a modifica��o em disco
		if (tabelaDePaginas[posicaoPaginaRemovida][BIT_MODIFICADA] == 1) {
			// Passa-se por par�metro o conte�do a ser salvo e o caminho do
			// arquivo de p�gina
			salvaConteudoDaPaginaModificadaEmDisco(memoriaPrincipal[posicaoLiberada].getConteudo(),
					memoriaVirtual[posicaoPaginaRemovida].getArquivoConteudo());

			// Volta-se o bit de modifica��o na tabela para 0
			tabelaDePaginas[posicaoPaginaRemovida][BIT_MODIFICADA] = 0;
		}
		// Apaga-se a pagina do frame na mem�ria principal
		memoriaPrincipal[posicaoLiberada] = null;

		// Zera-se o bit de presen�a na tabela
		tabelaDePaginas[posicaoPaginaRemovida][BIT_PRESENTE] = 0;
		tabelaDePaginas[posicaoPaginaRemovida][NUMERO_MOLDURA] = -1;

		// Zera-se os bits de referencia do frame que foi retirado
		for (int i = 0; i < QUANTIDADE_BITS_REFERENCIA; i++)
			bitsReferencia[posicaoLiberada][i] = 0;

		System.out.println("Frame " + posicaoLiberada + " liberado!");
		return posicaoLiberada;

	}

	private static void salvaConteudoDaPaginaModificadaEmDisco(Object conteudoPagina, String caminhoArquivoDePagina) {

		System.out.println("P�gina suja, salvando conte�do em disco...");

		try {
			FileOutputStream arquivo = new FileOutputStream(caminhoArquivoDePagina);
			ObjectOutputStream geradorDeArquivo = new ObjectOutputStream(arquivo);

			geradorDeArquivo.writeObject(conteudoPagina);
			geradorDeArquivo.flush();

			geradorDeArquivo.close();
			arquivo.close();

			System.out.println("O conte�do foi gravado no arquivo " + caminhoArquivoDePagina + " com sucesso");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
