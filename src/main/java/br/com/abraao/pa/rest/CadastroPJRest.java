package br.com.abraao.pa.rest;

import java.security.NoSuchAlgorithmException;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.abraao.pa.domain.Empresa;
import br.com.abraao.pa.domain.Funcionario;
import br.com.abraao.pa.domain.enums.PerfilEnum;
import br.com.abraao.pa.dtos.CadastroPJDto;
import br.com.abraao.pa.response.Response;
import br.com.abraao.pa.services.EmpresaService;
import br.com.abraao.pa.services.FuncionarioService;
import br.com.abraao.pa.utils.PasswordUtil;

@RestController
@RequestMapping("/api/cadastrar-pj")
@CrossOrigin(origins = "*")
public class CadastroPJRest {

	private static final Logger log = LoggerFactory.getLogger(CadastroPJRest.class);

	@Autowired
	private FuncionarioService funcionarioService;

	@Autowired
	private EmpresaService empresaService;

	public CadastroPJRest() {

	}

	/**
	 * Cadastra uma pessoa juridica no sistema
	 * 
	 * @param cadastroPJDto
	 * @param result
	 * @return RespinseEntity<Response<CadastroPJDto>>
	 * @throws NoSuchAlgorithmException
	 */

	@PostMapping
	public ResponseEntity<Response<CadastroPJDto>> cadastrar(@Valid @RequestBody CadastroPJDto cadastroPJDto,
			BindingResult result) throws NoSuchAlgorithmException {
		log.info("Cadastrando PJ: {}", cadastroPJDto.toString());
		Response<CadastroPJDto> response = new Response<CadastroPJDto>();

		validarDadosExistentes(cadastroPJDto, result);
		Empresa empresa = this.converterDtoParaEmpresa(cadastroPJDto);
		Funcionario funcionario = this.converterDtoParaFuncionario(cadastroPJDto, result);

		if (result.hasErrors()) {
			log.error("Erro validando dados de cadastro PJ: {}", result.getAllErrors());
			result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
			return ResponseEntity.badRequest().body(response);
		}

		this.empresaService.persistir(empresa);
		funcionario.setEmpresa(empresa);
		this.funcionarioService.persistir(funcionario);

		response.setData(this.converterCadastroPJDto(funcionario));
		return ResponseEntity.ok(response);

	}

	/**
	 * Verifica se a empresa ou funcionario já existem na base de dados.
	 * 
	 * @param cadastroPJDto
	 * @param result
	 */

	private void validarDadosExistentes(CadastroPJDto cadastroPJDto, BindingResult result) {

		this.empresaService.buscarPorCnpj(cadastroPJDto.getCnpj())
				.ifPresent(emp -> result.addError(new ObjectError("empresa", "Empresa já existe.")));

		this.funcionarioService.buscarPorCpf(cadastroPJDto.getCpf())
				.ifPresent(func -> result.addError(new ObjectError("funcionario", "CPF já existente.")));

		this.funcionarioService.buscarPorEmail(cadastroPJDto.getEmail())
				.ifPresent(func -> result.addError(new ObjectError("funcionario", "Email já existente")));

	}

	/**
	 * Converte os dados do DTO para empresa
	 * 
	 * @param cadastroPJDto
	 * @return Empresa
	 */

	private Empresa converterDtoParaEmpresa(CadastroPJDto cadastroPJDto) {
		Empresa empresa = new Empresa();
		empresa.setCnpj(cadastroPJDto.getCnpj());
		empresa.setRazaoSocial(cadastroPJDto.getRazaoSocial());

		return empresa;
	}

	/**
	 * Converte os dados DTO para Funcionario
	 * 
	 * @param cadastroPJDto
	 * @param result
	 * @return Funcionario
	 * @throws NoSuchAlgorithmException
	 */

	private Funcionario converterDtoParaFuncionario(CadastroPJDto cadastroPJDto, BindingResult result)
			throws NoSuchAlgorithmException {

		Funcionario funcionario = new Funcionario();
		funcionario.setNome(cadastroPJDto.getNome());
		funcionario.setEmail(cadastroPJDto.getEmail());
		funcionario.setCpf(cadastroPJDto.getCpf());
		funcionario.setPerfil(PerfilEnum.ROLE_ADMIN);
		funcionario.setSenha(PasswordUtil.gerarBCrypt(cadastroPJDto.getSenha()));

		return funcionario;
	}

	/**
	 * Popula o DTO de cadastro com os dados do funcionario e empresa
	 * 
	 * @param funcionario
	 * @return CadastroPJDto
	 */

	private CadastroPJDto converterCadastroPJDto(Funcionario funcionario) {

		CadastroPJDto cadastroPJDto = new CadastroPJDto();
		cadastroPJDto.setId(funcionario.getId());
		cadastroPJDto.setNome(funcionario.getNome());
		cadastroPJDto.setEmail(funcionario.getEmail());
		cadastroPJDto.setCpf(funcionario.getCpf());
		cadastroPJDto.setRazaoSocial(funcionario.getEmpresa().getRazaoSocial());
		cadastroPJDto.setCnpj(funcionario.getEmpresa().getCnpj());

		return cadastroPJDto;
	}

}
