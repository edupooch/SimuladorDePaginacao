package controle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import modelo.Frame;
import modelo.Pagina;

/**
 * CLASSE REPRESENTANDO A MMU - UNIDADE DE GERENCIAMENTO DE MEMÓRIA: Responsável
 * por manter a tabela de páginas virtuais e o mapeamento entre páginas e frames
 * 
 * @author edupooch
 *
 */
public class MMU {

	/**
	 * 1KB EM BYTES = 1024, número necessário para calcular a posição do frame
	 * em bytes para a instrução MOV REG
	 */
	public final static int KB_EM_BYTES = 1024;

	/**
	 * MEMÓRIA PRINCIPAL (MP) - Possui 64KF
	 * 
	 * A memória principal recebe um objeto do tipo FRAME, que possui o conteúdo
	 * de uma página
	 */
	static Frame[] memoriaPrincipal;


	/**
	 * MEMÓRIA VIRTUAL (MV)
	 * 
	 * Cada posição contém um objeto página, que possui uma string que
	 * referencia o caminho para um arquivo que tem o conteúdo da página
	 * 
	 */
	static Pagina[] memoriaVirtual;

	/**
	 * TABELA DE PÁGINAS - Contém as informações de mapeamento entre páginas e
	 * frames, tem o tamanho igual ao do número de páginas e 3 colunas com os
	 * valores dos ponteiros.
	 * 
	 * O índice do vetor representa o índice da memória virtual, e o valor
	 * dentro do vetor desse índice representa:
	 * 
	 * ÍNDICE 0 - NÚMERO DA POSIÇÃO DA MOLDURA NA MEMÓRIA PRINCIPAL;
	 * 
	 * INDICE 1 - BIT DE PRESENTE/AUSENTE;
	 * 
	 * INDICE 2 - BIT MODIFICADA.
	 * 
	 * 
	 * BITS DE REFERÊNCIA - MATRIZ SEPARADA DA TABELA DE PÁGINAS
	 * 
	 * PROTEÇÃO - ESCRITA/ LEITURA NÃO INCLUSO NESSE SISTEMA
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
	 * DA MEMORIA PRINCIPAL X 8 BITS QUE SERÃO GUARDADOS, possui as informações
	 * de quais as posições da memória principal que foram referenciadas nos
	 * últimos 8 ciclos, a com o menor valor é retirada segundo a lógica do
	 * algoritmo aging.
	 */
	static int[][] bitsReferencia;
	private final static int QUANTIDADE_BITS_REFERENCIA = 8;
	
	
	
	private static int posicaoSolicitada;

	/**
	 * Variável para definir o tipo de acesso que o processador solicitou
	 */
	public final static int GRAVAR_TEMPO = 0;

	/**
	 * Tamanho da página
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
		
		System.out.println("Solicitação de acesso (lógico) do processador: MOV REG, "
				+ posicaoSolicitada * tamanhoPagina * KB_EM_BYTES + " (POSIÇÃO " + posicaoSolicitada +")");
		
		Pagina pagina = memoriaVirtual[posicaoSolicitada];

		if (tabelaDePaginas[posicaoSolicitada][BIT_PRESENTE] == 1) {
			hit = true;
			System.out.println("Página presente na memória principal na posição "
					+ tabelaDePaginas[posicaoSolicitada][NUMERO_MOLDURA]);
			bitsReferencia[tabelaDePaginas[posicaoSolicitada][NUMERO_MOLDURA]][0] = 1;

		} else {
			hit = false;
			// A página não está carregada na memória principal e precisa ser
			// carregada
			System.out.println("Página ausente na memória principal!");
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

		System.out.println("Solicitação de acesso (físico) do processador: MOV REG, "
				+ posicaoFisica * tamanhoPagina * KB_EM_BYTES + " (POSIÇÃO " + posicaoFisica + ")");

		return hit;
	}

	/**
	 * Procura um frame disponível na memória principal e salva a página naquele
	 * frame, como ela é referenciada nessa instrução, o método grava 1 no array
	 * de bits R na primeira posição do frame disponivel
	 * 
	 * @param pagina
	 */
	private static void carregaPaginaNaMP(Pagina pagina) {

		int frameDisponivel = procuraFrameDisponivel();

		// Passa o conteúdo do disco para o frame
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

		// Salva a informação do numero da moldura na tabela de páginas
		tabelaDePaginas[posicaoSolicitada][NUMERO_MOLDURA] = frameDisponivel;
		// Coloca na tabela que a pagina está presente na memória principal
		tabelaDePaginas[posicaoSolicitada][BIT_PRESENTE] = 1;

		System.out.println("Página carregada na posição " + frameDisponivel);

		// Atualiza a matriz de bits de referencia, colocando 1 para a
		// solicitada
		bitsReferencia[frameDisponivel][0] = 1;

		// Print da matriz de bits após a realocação e referencia da pagina nova
		System.out.println("\nBits de referência:");
		for (int i = 0; i < bitsReferencia.length; i++) {
			System.out.print("Posição " + i + ": ");
			for (int j = 0; j < QUANTIDADE_BITS_REFERENCIA; j++)
				System.out.print(bitsReferencia[i][j] + " ");
			System.out.println("");
		}

	}

	/**
	 * Realoca os bits R de todas as páginas da memoria fisica
	 */
	private static void realocaBitsR() {
		// Realocar bits pra direita
		for (int i = 0; i < bitsReferencia.length; i++) {
			for (int j = QUANTIDADE_BITS_REFERENCIA - 1; j > 0; j--)
				bitsReferencia[i][j] = bitsReferencia[i][j - 1];
			// Primeira posição é zerada, será marcado um para os referenciados
			// no ciclo
			bitsReferencia[i][0] = 0;
		}
	}

	private static int procuraFrameDisponivel() {
		System.out.println("Procurando frame disponível...");
		for (int i = 0; i < memoriaPrincipal.length; i++) {
			if (memoriaPrincipal[i] == null) {
				System.out.println("Frame " + i + " disponível!");
				return i;
				// Caso haja alguma página disponível o método para a execução
				// aqui e retorna o número desse frame, se não a execução
				// continua para liberar um frame
			}
		}
		System.out.println("Não existem frames disponíveis!");
		int frameLiberado = liberaUmFrame();
		return frameLiberado;

	}

	private static int liberaUmFrame() {
		System.out.println("Verificando o melhor frame para ser liberado...");
		// Criando um array com 8 valores de referência para verificar depois
		// qual foi a posição menos referenciada
		int[] valores = new int[memoriaPrincipal.length];
		System.out.println("Matriz dos bits de referência:");
		for (int i = 0; i < bitsReferencia.length; i++) {
			System.out.print("Posição " + i + ": ");
			for (int j = 0; j < QUANTIDADE_BITS_REFERENCIA; j++) {
				// Valor salvo no array valores corresponde a uma sequência de
				// binários, multplica-se pelo 10^j para se obter um número de
				// até 8 dígitos ex 10001010, representando as referências nos
				// últimos ciclos
				valores[i] += bitsReferencia[i][j] * Math.pow(10, 7 - j);
				System.out.print(bitsReferencia[i][j] + " ");
			}
			System.out.println("");
		}

		// Pegar a posição com o menor valor dentro do array valores para ver
		// qual é a posição que deverá ser liberada
		int minimo = valores[0];
		int posicaoLiberada = 0;
		for (int i = 1; i < valores.length; i++) {
			if (valores[i] < minimo) {
				minimo = valores[i];
				posicaoLiberada = i;
			}
		}
		System.out.println("Posição " + posicaoLiberada + " tem o menor valor, portanto será liberada");

		// Verificando qual foi a página virtual que foi removida da memória
		// fisica
		int posicaoPaginaRemovida = -1;
		for (int i = 0; i < tabelaDePaginas.length; i++) {
			if (tabelaDePaginas[i][NUMERO_MOLDURA] == posicaoLiberada) {
				posicaoPaginaRemovida = i;
			}
		}

		System.out.println("Verificando se a página foi modificada...");
		System.out.println("Bit de modificação igual a " + tabelaDePaginas[posicaoPaginaRemovida][BIT_MODIFICADA]);

		// Caso o bit sujo seja 1, salva-se a modificação em disco
		if (tabelaDePaginas[posicaoPaginaRemovida][BIT_MODIFICADA] == 1) {
			// Passa-se por parâmetro o conteúdo a ser salvo e o caminho do
			// arquivo de página
			salvaConteudoDaPaginaModificadaEmDisco(memoriaPrincipal[posicaoLiberada].getConteudo(),
					memoriaVirtual[posicaoPaginaRemovida].getArquivoConteudo());

			// Volta-se o bit de modificação na tabela para 0
			tabelaDePaginas[posicaoPaginaRemovida][BIT_MODIFICADA] = 0;
		}
		// Apaga-se a pagina do frame na memória principal
		memoriaPrincipal[posicaoLiberada] = null;

		// Zera-se o bit de presença na tabela
		tabelaDePaginas[posicaoPaginaRemovida][BIT_PRESENTE] = 0;
		tabelaDePaginas[posicaoPaginaRemovida][NUMERO_MOLDURA] = -1;

		// Zera-se os bits de referencia do frame que foi retirado
		for (int i = 0; i < QUANTIDADE_BITS_REFERENCIA; i++)
			bitsReferencia[posicaoLiberada][i] = 0;

		System.out.println("Frame " + posicaoLiberada + " liberado!");
		return posicaoLiberada;

	}

	private static void salvaConteudoDaPaginaModificadaEmDisco(Object conteudoPagina, String caminhoArquivoDePagina) {

		System.out.println("Página suja, salvando conteúdo em disco...");

		try {
			FileOutputStream arquivo = new FileOutputStream(caminhoArquivoDePagina);
			ObjectOutputStream geradorDeArquivo = new ObjectOutputStream(arquivo);

			geradorDeArquivo.writeObject(conteudoPagina);
			geradorDeArquivo.flush();

			geradorDeArquivo.close();
			arquivo.close();

			System.out.println("O conteúdo foi gravado no arquivo " + caminhoArquivoDePagina + " com sucesso");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
