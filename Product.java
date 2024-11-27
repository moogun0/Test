package com.korebap.app.view.async;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.korebap.app.biz.product.ProductDTO;
import com.korebap.app.biz.product.ProductService;


// Product 관련 비동기 모아둔 class

@RestController
public class Product {

	@Autowired
	ProductService productService;

	@RequestMapping(value="/productList.do", method=RequestMethod.GET)
	public @ResponseBody Map<String, Object> productPage(ProductDTO productDTO,
			@RequestParam(value="currentPage", required = false, defaultValue = "1") int current_page) {

		// [ 상품 페이지네이션]
		System.out.println("=====com.korebap.app.view.async productPage 비동기 시작");
		System.out.println("=====com.korebap.app.view.async productPage 비동기 currentPage ["+current_page+"]");


		// V에서 받아온 파라미터 로그
		System.out.println("=====com.korebap.app.view.async productPage 비동기 product_searchKeyword ["+productDTO.getProduct_searchKeyword()+"]");
		System.out.println("=====com.korebap.app.view.async productPage 비동기 product_location ["+productDTO.getProduct_location()+"]");
		System.out.println("=====com.korebap.app.view.async productPage 비동기 product_category ["+productDTO.getProduct_category()+"]");
		System.out.println("=====com.korebap.app.view.async productPage 비동기 product_search_criteria ["+productDTO.getProduct_search_criteria()+"]");
		System.out.println("=====com.korebap.app.view.async productPage 비동기 Product_page_num ["+productDTO.getProduct_page_num()+"]");
		System.out.println("=====com.korebap.app.view.async productPage 비동기 Product_categories ["+productDTO.getProduct_categories()+"]");
		System.out.println("=====com.korebap.app.view.async productPage 비동기 Product_types ["+productDTO.getProduct_types()+"]");

		System.out.println("비동기 로그 productDTO ["+productDTO+"]");
		List<ProductDTO> productList=null;
		int product_total_page=0;
		Map<String, Object> responseMap = new HashMap<>();
		//필터 
		if(productDTO.getProduct_categories()!= null&& !productDTO.getProduct_categories().isEmpty()||
				productDTO.getProduct_types()!=null&& !productDTO.getProduct_types().isEmpty()||
				productDTO.getProduct_searchKeyword()!=null&&!productDTO.getProduct_searchKeyword().isEmpty()) {
			productDTO.setProduct_condition("PRODUCT_SELECTALL_SEARCH");
			productList = productService.selectAll(productDTO);

		}
		else {
			// 카테고리와 위치에 대한 기본값 설정
			if (productDTO.getProduct_category() == null || productDTO.getProduct_category().isEmpty()) {
				productDTO.setProduct_category(null); // 필터 적용하지 않음
			}

			if (productDTO.getProduct_location() == null || productDTO.getProduct_location().isEmpty()) {
				productDTO.setProduct_location(null); // 필터 적용하지 않음
			}


			//M에게 데이터를 보내주고, 결과를 ArrayList로 반환받는다. 
			productDTO.setProduct_page_num(current_page);
			productList = productService.selectAll(productDTO);

			System.out.println("비동기 로그 productList ["+productList+"]");


			// [게시판 페이지 전체 개수]

			productDTO.setProduct_condition("PRODUCT_PAGE_COUNT");
			productDTO = productService.selectOne(productDTO);

			// int 타입 변수에 받아온 값을 넣어준다.
			product_total_page = productDTO.getProduct_total_page();

			System.out.println("productList 로그 product_total_page ["+product_total_page+"]");

		}
		// 결과를 Map에 담아 반환
		responseMap.put("productList", productList);
		responseMap.put("product_page_count", product_total_page);
		responseMap.put("currentPage", current_page);


		System.out.println("로그!!!!!!!! responseMap : ["+responseMap+"]");
		System.out.println("로그!!!!!!!! productList : ["+productList+"]");

		// 현재 페이지 > 전체 페이지
		if (current_page > product_total_page) {
			System.out.println("마지막 페이지 요청, 더 이상 데이터 없음");
			
			
			responseMap.put("message", "마지막 페이지입니다.");
			responseMap.put("productList", List.of()); // 빈 리스트 반환
			responseMap.put("product_page_count", product_total_page);
			responseMap.put("currentPage", current_page);
			
			return responseMap;
		}
		return responseMap; 
	}

}
