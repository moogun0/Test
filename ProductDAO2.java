package com.korebap.app.biz.product;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProductDAO2 {
	// 상품 등록
	private final String PRODUCT_INSERT = "INSERT INTO PRODUCT (PRODUCT_NAME, PRODUCT_PRICE, PRODUCT_DETAILS, PRODUCT_ADDRESS, PRODUCT_LOCATION, PRODUCT_CATEGORY, PRODUCT_SELLER_ID) \r\n"
			+ "VALUES (?, ?, ?, ?, ?, ?, ?)";
	// 사장님 상품 수정
	private final String PRODUCT_UPDATE = "UPDATE PRODUCT P JOIN MEMBER M ON M.MEMBER_ID = P.PRODUCT_SELLER_ID\r\n"
			+ "SET P.PRODUCT_NAME = ?, P.PRODUCT_PRICE = ?, P.PRODUCT_DETAILS = ?, P.PRODUCT_ADDRESS = ?, P.PRODUCT_LOCATION = ?, P.PRODUCT_CATEGORY = ? WHERE P.PRODUCT_SELLER_ID = ?";

	// 전체출력 통합 >> 정렬기준 + 검색어
	private final String PRODUCT_SELECTALL = "SELECT PRODUCT_NUM, PRODUCT_NAME, PRODUCT_PRICE, PRODUCT_ADDRESS, PRODUCT_LOCATION, PRODUCT_CATEGORY, RATING, PAYMENT_COUNT, WISHLIST_COUNT, FILE_DIR"
			+ " FROM ("
			+ "    SELECT PRODUCT_NUM, PRODUCT_NAME, PRODUCT_PRICE, PRODUCT_ADDRESS, PRODUCT_LOCATION, PRODUCT_CATEGORY, RATING, PAYMENT_COUNT, WISHLIST_COUNT,FILE_DIR,"
			+ "           ROW_NUMBER() OVER (" + "           		ORDER BY" + "           		CASE"
			+ "           		 	WHEN ?  = 'newest' THEN PRODUCT_NUM"
			+ "                    WHEN ? = 'rating' THEN COALESCE(RATING, -1)"
			+ "                    WHEN ? = 'wish' THEN COALESCE(WISHLIST_COUNT, -1)"
			+ "                    WHEN ? = 'payment' THEN COALESCE(PAYMENT_COUNT, -1)"
			+ "                    ELSE PRODUCT_NUM" + "                   END DESC) AS ROW_NUM"
			+ "    FROM PRODUCT_INFO_VIEW" + "    WHERE PRODUCT_NAME LIKE CONCAT('%',COALESCE(?, ''), '%')"
			+ "    AND (PRODUCT_LOCATION = COALESCE(?, PRODUCT_LOCATION)) AND (PRODUCT_CATEGORY = COALESCE(?, PRODUCT_CATEGORY)) "
			+ ") AS subquery " + "WHERE ROW_NUM BETWEEN (COALESCE(?, 1) - 1) * 9 + 1 AND COALESCE(?, 1) * 9";

	// 상품 상세보기
	private final String SELECTONE = "SELECT PRODUCT_NUM, PRODUCT_NAME, PRODUCT_PRICE, PRODUCT_DETAILS, PRODUCT_ADDRESS, PRODUCT_LOCATION, PRODUCT_CATEGORY, "
			+ "(SELECT COALESCE(ROUND(AVG(R.REVIEW_STAR), 1), 0) FROM REVIEW R WHERE R.REVIEW_PRODUCT_NUM = PRODUCT_NUM) AS RATING, "
			+ "(SELECT COUNT(PA.PAYMENT_PRODUCT_NUM) FROM PAYMENT PA WHERE PA.PAYMENT_PRODUCT_NUM = PRODUCT_NUM) AS PAYMENT_COUNT, "
			+ "(SELECT COUNT(W.WISHLIST_PRODUCT_NUM) FROM WISHLIST W WHERE W.WISHLIST_PRODUCT_NUM = PRODUCT_NUM) AS WISHLIST_COUNT "
			+ "FROM PRODUCT WHERE PRODUCT_NUM = ?";

	// 사용자가 선택한 일자의 재고 보기
	private final String SELECTONE_CURRENT_STOCK = "SELECT P.PRODUCT_NUM, (P.PRODUCT_CNT - COALESCE(RS.RESERVATION_COUNT, 0)) AS CURRENT_STOCK "
			+ "FROM PRODUCT P "
			+ "LEFT JOIN (SELECT PA.PAYMENT_PRODUCT_NUM AS PRODUCT_NUM, COUNT(R.RESERVATION_REGISTRATION_DATE) AS RESERVATION_COUNT "
			+ "FROM RESERVATION R " + "JOIN PAYMENT PA ON R.RESERVATION_PAYMENT_NUM = PA.PAYMENT_NUM "
			+ "WHERE R.RESERVATION_REGISTRATION_DATE = ? "
			+ "GROUP BY PA.PAYMENT_PRODUCT_NUM) RS ON P.PRODUCT_NUM = RS.PRODUCT_NUM " + "WHERE P.PRODUCT_NUM = ?";

	// 전체 데이터 개수를 반환 (전체 페이지 수 - 기본)
	private final String PRODUCT_TOTAL_PAGE = "SELECT CEIL(COALESCE(COUNT(PRODUCT_NUM), 0) / 9.0) AS PRODUCT_TOTAL_PAGE FROM PRODUCT WHERE 1 = 1";

	// 전체 데이터 개수를 반환 (검색어 사용 페이지수)
	private final String PRODUCT_SEARCH_PAGE = "AND PRODUCT_NAME LIKE CONCAT('%', ?, '%')";

	// 전체 데이터 개수를 반환 (필터링 검색 페이지 수)
	private final String PRODUCT_FILTERING_PAGE = "AND (PRODUCT_LOCATION = COALESCE(?, PRODUCT_LOCATION) AND PRODUCT_CATEGORY = COALESCE(?, PRODUCT_CATEGORY))";

	// 샘플데이터(크롤링) insert
	// 바다
	// 낚시배
	private final String PRODUCT_CRAWLING_SEA_BOAR_INSERT = "INSERT INTO PRODUCT (PRODUCT_NAME, PRODUCT_PRICE, PRODUCT_DETAILS, PRODUCT_CNT, PRODUCT_ADDRESS, PRODUCT_LOCATION, PRODUCT_CATEGORY,PRODUCT_SELLER_ID) "
			+ "VALUES (?, ?, ?, ?, ?, '바다', '낚시배',?)";

	// 낚시터
	private final String PRODUCT_CRAWLING_SEA_FISHING_INSERT = "INSERT INTO PRODUCT (PRODUCT_NAME, PRODUCT_PRICE, PRODUCT_DETAILS, PRODUCT_CNT, PRODUCT_ADDRESS, PRODUCT_LOCATION, PRODUCT_CATEGORY,PRODUCT_SELLER_ID) "
			+ "VALUES (?, ?, '바다 낚시터입니다~!', 99, ?, '바다', '낚시터',?)";

	// 민물
	// 낚시터
	private final String PRODUCT_CRAWLING_FRESH_WATER_FISHING_INSERT = "INSERT INTO PRODUCT (PRODUCT_NAME, PRODUCT_PRICE, PRODUCT_DETAILS, PRODUCT_CNT, PRODUCT_ADDRESS, PRODUCT_LOCATION, PRODUCT_CATEGORY,PRODUCT_SELLER_ID) "
			+ "VALUES (?, ?, '민물 낚시터입니다~!', ?, ?, '민물', '수상',?)";

	// 낚시카페
	private final String PRODUCT_CRAWLING_FRESH_WATER_FISHING_CAFE_INSERT = "INSERT INTO PRODUCT (PRODUCT_NAME, PRODUCT_PRICE, PRODUCT_DETAILS, PRODUCT_CNT, PRODUCT_ADDRESS, PRODUCT_LOCATION, PRODUCT_CATEGORY,PRODUCT_SELLER_ID) "
			+ "VALUES (?, ?, '민물 낚시카페입니다~!', 50, ?, '민물', '낚시카페',?)";

	// 이미지 파일 저장을 위한 select
	// 상품 pk 출력
	private final String PRODUCT_NUM_SELECT = "SELECT PRODUCT_NUM AS MAX_NUM \r\n"
			+ "FROM PRODUCT \r\n"
			+ "ORDER BY PRODUCT_NUM DESC \r\n"
			+ "LIMIT 1";
	// 상품 번호 출력
	private final String PRODUCT_SELECTALL_CRAWLING = "SELECT PRODUCT_NUM FROM PRODUCT";

	// 필터링 쿼리 정의 (인스턴스 변수)
	// 필터 검색
	private final String PRODUCT_FILTER_SEARCH = "SELECT PRODUCT_NUM, PRODUCT_NAME, PRODUCT_PRICE, PRODUCT_ADDRESS, PRODUCT_LOCATION, PRODUCT_CATEGORY, RATING, PAYMENT_COUNT, WISHLIST_COUNT, FILE_DIR "+
			"FROM ( " + 
			"SELECT P.PRODUCT_NUM, P.PRODUCT_NAME, P.PRODUCT_PRICE, P.PRODUCT_DETAILS, " +
			"P.PRODUCT_CNT, P.PRODUCT_ADDRESS, P.PRODUCT_LOCATION, P.PRODUCT_CATEGORY, " +
			"IFNULL(I.FILE_DIR, '') AS FILE_DIR, " +  // 이미지 파일 경로
			"COALESCE(ROUND(AVG(R.REVIEW_STAR), 1), 0) AS RATING, " +  // 리뷰 별점 평균
			"COALESCE(SUM(W.WISHLIST_COUNT), 0) AS WISHLIST_COUNT, " +  // 위시리스트 수
			"COALESCE(COUNT(PT.PAYMENT_NUM), 0) AS PAYMENT_COUNT, " +  // 결제 수
			"ROW_NUMBER() OVER (ORDER BY P.PRODUCT_NUM) AS ROW_NUM " +  // ROW_NUMBER
			"FROM PRODUCT P " +  // 상품 테이블
			"LEFT JOIN (SELECT REVIEW_PRODUCT_NUM, AVG(REVIEW_STAR) AS REVIEW_STAR FROM REVIEW GROUP BY REVIEW_PRODUCT_NUM) R " +  // 리뷰 평균
			"ON P.PRODUCT_NUM = R.REVIEW_PRODUCT_NUM " +  // 상품 번호로 조인
			"LEFT JOIN (SELECT WISHLIST_PRODUCT_NUM, COUNT(*) AS WISHLIST_COUNT FROM WISHLIST GROUP BY WISHLIST_PRODUCT_NUM) W " +  // 위시리스트 집계
			"ON P.PRODUCT_NUM = W.WISHLIST_PRODUCT_NUM " +  // 상품 번호로 조인
			"LEFT JOIN PAYMENT PT ON P.PRODUCT_NUM = PT.PAYMENT_PRODUCT_NUM " +  // 결제 정보 조인
			"LEFT JOIN ( " +
			"   SELECT I1.PRODUCT_ITEM_NUM, I1.FILE_DIR FROM imagefile I1 " +
			"   WHERE I1.FILE_NUM = ( " +
			"       SELECT I2.FILE_NUM FROM imagefile I2 " +
			"       WHERE I2.PRODUCT_ITEM_NUM = I1.PRODUCT_ITEM_NUM " +
			"       ORDER BY I2.FILE_NUM " +  // 이미지 번호로 정렬
			"       LIMIT 1 OFFSET 1" +  // 두 번째 이미지 선택
			"   ) " +
			") I ON P.PRODUCT_NUM = I.PRODUCT_ITEM_NUM " +  // 두 번째 이미지 조인
			"WHERE 1=1 ";  // 기본 조건 (모든 결과 포함)

	// 위치 필터링 쿼리
	private final String TYPE_FILTER = "AND P.PRODUCT_LOCATION IN (%s) ";
	// 카테고리 필터링 쿼리
	private final String CATEGORY_FILTER = "AND P.PRODUCT_CATEGORY IN (%s) ";  
	// 제품 이름 검색 쿼리
	private final String NAME_SEARCH_FILTER = "AND P.PRODUCT_NAME LIKE ? "; 
	// GROUP BY 절에 필요한 필드
	private final String GROUP_BY = "GROUP BY P.PRODUCT_NUM, P.PRODUCT_NAME, P.PRODUCT_PRICE, P.PRODUCT_DETAILS, "
			+ "P.PRODUCT_CNT, P.PRODUCT_ADDRESS, P.PRODUCT_LOCATION, P.PRODUCT_CATEGORY, "
			+ "I.FILE_DIR ";  
	// 페이지네이션을 위한 쿼리
	private final String FILTER_PAGE = ") AS SUBQUERY WHERE ROW_NUM BETWEEN (COALESCE(?, 1) - 1) * 9 + 1 AND COALESCE(?, 1) * 9";
	// 페이지네이션 : 페이지 개수

	@Autowired
	private JdbcTemplate jdbcTemplate;


	public boolean insert(ProductDTO productDTO) {
		System.out.println("model.ProductDAO.insert 시작");

		// 변수 초기화
		String product_name = productDTO.getProduct_name();
		int product_price = productDTO.getProduct_price();
		String product_details = productDTO.getProduct_details();
		int product_cnt = productDTO.getProduct_cnt();
		String product_address = productDTO.getProduct_address();
		String product_location = productDTO.getProduct_location();
		String product_category = productDTO.getProduct_category();
		String product_seller_id = productDTO.getProduct_seller_id();

		// 쿼리 및 파라미터 초기화
		String sql = null;
		Object[] args = null;
		System.out.println("====model.ProductDAO2.insert productDTO.getProduct_condition() : ["
				+ productDTO.getProduct_condition() + "]");
		if (productDTO.getProduct_condition().equals("PRODUCT_INSERT")) { // 상품등록
			sql = PRODUCT_INSERT;
			args = new Object[] { product_name, // 상품명
					product_price, // 가격
					product_details, // 설명
					product_cnt, product_address, // 주소
					product_location, // 위치
					product_category, // 카테고리
					product_seller_id // 판매자
			};
		} 
		else if (productDTO.getProduct_condition().equals("PRODUCT_CRAWLING_SEA_BOAR_INSERT")) {
			System.out.println("model.ProductDAO.insert 바다 낚시배 시작");
			sql = PRODUCT_CRAWLING_SEA_BOAR_INSERT;
			args = new Object[]{
					product_name,   // 상품명
					product_price,   // 가격
					product_details, // 설명
					product_cnt,     // 재고
					product_address,   // 주소
					product_seller_id	// 사장님 id
			};
		} else if (productDTO.getProduct_condition().equals("PRODUCT_CRAWLING_SEA_FISHING_INSERT")) {
			System.out.println("model.ProductDAO.insert 바다 낚시터 시작");
			sql = PRODUCT_CRAWLING_SEA_FISHING_INSERT;
			args = new Object[]{
					product_name,   // 상품명
					product_price,   // 가격
					product_address,   // 주소
					product_seller_id	// 사장님 id
			};
		} else if (productDTO.getProduct_condition().equals("PRODUCT_CRAWLING_FRESH_WATER_FISHING_INSERT")) {
			System.out.println("model.ProductDAO.insert 민물 낚시터 시작");
			sql = PRODUCT_CRAWLING_FRESH_WATER_FISHING_INSERT;
			args = new Object[]{
					product_name,   // 상품명
					product_price,   // 가격
					product_cnt,     // 수량
					product_address,   // 주소
					product_seller_id	// 사장님 id
			};
		} else if (productDTO.getProduct_condition().equals("PRODUCT_CRAWLING_FRESH_WATER_FISHING_CAFE_INSERT")) {
			System.out.println("model.ProductDAO.insert 민물 낚시카페 시작");
			sql = PRODUCT_CRAWLING_FRESH_WATER_FISHING_CAFE_INSERT;
			args = new Object[]{
					product_name,   // 상품명
					product_price,   // 가격
					product_address,   // 주소
					product_seller_id	// 사장님 id
			};
		}
		// 쿼리 실행
		int result = jdbcTemplate.update(sql, args);
		System.out.println("model.ProductDAO.insert result : " + result);

		if (result <= 0) {
			System.out.println("model.ProductDAO.insert 행 변경 실패");
			return false;
		}

		System.out.println("model.ProductDAO.insert 종료");    
		return true; // 성공적으로 삽입 완료
	}


	public List<ProductDTO> selectAll(ProductDTO productDTO) { // 전체 출력
		System.out.println("model.ProductDAO.selectAll 시작");
		List<ProductDTO> datas = new ArrayList<>();
		
		System.out.println("ProductDAO.selectAll productDTO.getProduct_condition() :"+productDTO.getProduct_condition());
		// 상품 전체 출력
		if (productDTO.getProduct_condition()==null|| productDTO.getProduct_condition().isEmpty()){
			System.out.println("model.ProductDAO.selectAll 상품 전체 목록 출력 (최신순) 시작");
			Object[] args = {
					productDTO.getProduct_search_criteria(),  // 검색 정렬 기준
					productDTO.getProduct_search_criteria(),  // 검색 정렬 기준
					productDTO.getProduct_search_criteria(),  // 검색 정렬 기준
					productDTO.getProduct_search_criteria(),  // 검색 정렬 기준
					productDTO.getProduct_searchKeyword(), // 검색어
					productDTO.getProduct_location(), // 상품 장소 (바다/민물)
					productDTO.getProduct_category(), // 상품 유형 (낚시배/낚시터/낚시카페/수상)
					productDTO.getProduct_page_num(), // 페이지 번호 (첫 데이터)
					productDTO.getProduct_page_num()   // 페이지 번호 (마지막 데이터)
			};
			datas = jdbcTemplate.query(PRODUCT_SELECTALL, args, new ProductRowMapper_all());
		}
		else if (productDTO.getProduct_condition().equals("PRODUCT_SELECTALL_CRAWLING")) { // 크롤링 번호 확인			
			System.out.println("model.ProductDAO.selectAll_PRODUCT_SELECTALL_CRAWLING 시작");
			datas = jdbcTemplate.query(PRODUCT_SELECTALL_CRAWLING,new ProductRowMapper_all_crawling());
		}
		else if (productDTO.getProduct_condition().equals("PRODUCT_SELECTALL_SEARCH")) {
		    System.out.println("model.ProductDAO.selectAll_PRODUCT_SELECTALL_SEARCH 시작");

		    StringBuilder sql = new StringBuilder(PRODUCT_FILTER_SEARCH);  // 기본 쿼리 초기화
		    List<Object> params = new ArrayList<>();  // 쿼리 파라미터를 저장할 리스트

		    // 유형 필터링
		    if (productDTO.getProduct_types() != null && !productDTO.getProduct_types().isEmpty()) {
		        System.out.println("유형 필터링 조건 추가: " + productDTO.getProduct_types());
		        String placeholders = String.join(", ", Collections.nCopies(productDTO.getProduct_types().size(), "?"));
		        sql.append(String.format(TYPE_FILTER, placeholders));
		        params.addAll(productDTO.getProduct_types());
		    }

		    // 카테고리 필터링
		    if (productDTO.getProduct_categories() != null && !productDTO.getProduct_categories().isEmpty()) {
		        System.out.println("카테고리 필터링 조건 추가: " + productDTO.getProduct_categories());
		        String placeholders = String.join(", ", Collections.nCopies(productDTO.getProduct_categories().size(), "?"));
		        sql.append(String.format(CATEGORY_FILTER, placeholders));
		        params.addAll(productDTO.getProduct_categories());
		    }

		    // 검색 쿼리 필터링
		    if (productDTO.getProduct_searchKeyword() != null && !productDTO.getProduct_searchKeyword().isEmpty()) {
		        System.out.println("검색어 조건 추가: " + productDTO.getProduct_searchKeyword());
		        sql.append(NAME_SEARCH_FILTER);
		        params.add("%" + productDTO.getProduct_searchKeyword() + "%");
		    }

		    sql.append(GROUP_BY);  // GROUP BY 절 추가 

		    // 페이지네이션
		    int pageNum = productDTO.getProduct_page_num() > 0 ? productDTO.getProduct_page_num() : 1; 
		    int itemsPerPage = 9;
		    sql.append(FILTER_PAGE);  
//		    params.add((pageNum - 1) * itemsPerPage + 1); // 시작 번호
//		    params.add(pageNum * itemsPerPage); // 끝 번호
		    params.add(pageNum); // 시작 번호
		    params.add(pageNum); // 끝 번호

		    // 카운트 쿼리 초기화
		    String countQuery = "SELECT COUNT(*) FROM PRODUCT P WHERE 1=1 "; // 기본 조건

		    // 카운트 쿼리 조건 추가
		    List<Object> countParams = new ArrayList<>(); // 카운트 파라미터 리스트 초기화
		    if (productDTO.getProduct_types() != null && !productDTO.getProduct_types().isEmpty()) {
		        String placeholders = String.join(", ", Collections.nCopies(productDTO.getProduct_types().size(), "?"));
		        countQuery += String.format(TYPE_FILTER, placeholders);
		        countParams.addAll(productDTO.getProduct_types());
		    }
		    if (productDTO.getProduct_categories() != null && !productDTO.getProduct_categories().isEmpty()) {
		        String placeholders = String.join(", ", Collections.nCopies(productDTO.getProduct_categories().size(), "?"));
		        countQuery += String.format(CATEGORY_FILTER, placeholders);
		        countParams.addAll(productDTO.getProduct_categories());
		    }
		    if (productDTO.getProduct_searchKeyword() != null && !productDTO.getProduct_searchKeyword().isEmpty()) {
		        countQuery += NAME_SEARCH_FILTER;
		        countParams.add("%" + productDTO.getProduct_searchKeyword() + "%");
		    }

		    // 카운트 쿼리 실행 및 결과 반환
		    int totalCount = 0;
		    try {
		        totalCount = jdbcTemplate.queryForObject(countQuery, Integer.class, countParams.toArray());
		    } catch (Exception e) {
		        System.err.println("카운트 쿼리 실행 중 오류 발생: " + e.getMessage());
		        return null; // 빈 리스트 반환
		    }

		    // 총 페이지 수 계산
		    int totalPages = (int) Math.ceil((double) totalCount / itemsPerPage);
		    
		    // productDTO에 값 설정
		    productDTO.setProduct_total_page(totalPages); // 총 페이지 수 설정

		    // 실제 쿼리 실행
		    try {
		        datas = jdbcTemplate.query(sql.toString(), new ProductRowMapper_filter_all(), params.toArray());
		    } catch (Exception e) {
		        System.err.println("쿼리 실행 중 오류 발생: " + e.getMessage());
		        return null; // 빈 리스트 반환
		    }

		System.out.println("model.ProductDAO.selectAll 결과: " + datas);
	}
		return datas; // 최종 결과 반환
	}

	public ProductDTO selectOne(ProductDTO productDTO) { // 한개 출력
		System.out.println("model.ProductDAO.selectOne 시작");
		ProductDTO data = null;
		//ProductRowMapper productRowMapper = new ProductRowMapper(); // 미리 생성
		System.out.println("ProductDAO.selectOne productDTO.getProduct_condition() :"+productDTO.getProduct_condition());

		try {
			// 상품 상세보기
			if (productDTO.getProduct_condition().equals("PRODUCT_BY_INFO")) { 
				System.out.println("model.ProductDAO.selectOne 상품 상세보기 쿼리 읽기 시작");
				Object[] args = { productDTO.getProduct_num() }; // 상품번호
				data = jdbcTemplate.queryForObject(SELECTONE, args, new ProductRowMapper_one_by_info());
			}
			// 사용자가 선택한 일자의 재고 보기
			else if (productDTO.getProduct_condition().equals("PRODUCT_BY_CURRENT_STOCK")) { 
				System.out.println("model.ProductDAO.selectOne 선택일자 재고보기 쿼리 읽기 시작");
				Object[] args = { productDTO.getProduct_reservation_date(), productDTO.getProduct_num() };
				data = jdbcTemplate.queryForObject(SELECTONE_CURRENT_STOCK, args, new ProductRowMapper_one_current_stock());
			}
			// 크롤링 상품번호 조회
			else if (productDTO.getProduct_condition().equals("PRODUCT_NUM_SELECT")) { 
				System.out.println("model.ProductDAO.selectOne 크롤링 상품번호 쿼리 읽기 시작");
				data = jdbcTemplate.queryForObject(PRODUCT_NUM_SELECT,new ProductRowMapper_one_num());
			}

			// 페이지네이션에 사용하기 위해 전체 페이지 수 반환
			else if (productDTO.getProduct_condition().equals("PRODUCT_PAGE_COUNT")) {
				System.out.println("model.ProductDAO.selectOne 전체 상품 페이지 수 반환 쿼리 읽기 시작");
				Object[] args;
				// 검색어, 위치, 카테고리 모두 있는 경우
				if (productDTO.getProduct_searchKeyword() != null && !productDTO.getProduct_searchKeyword().isEmpty() &&
						(productDTO.getProduct_location() != null && !productDTO.getProduct_location().isEmpty()) ||
						(productDTO.getProduct_category() != null && !productDTO.getProduct_category().isEmpty()) ) {

					System.out.println("model.ProductDAO.selectOne 검색어, 위치, 카테고리 모두 있는 경우");
					args = new Object[]{
							productDTO.getProduct_searchKeyword(),
							productDTO.getProduct_location(),
							productDTO.getProduct_category()
					};
					data = jdbcTemplate.queryForObject(PRODUCT_TOTAL_PAGE + " "+PRODUCT_SEARCH_PAGE+" "+PRODUCT_FILTERING_PAGE, args, new ProductRowMapper_one_page_count());      
				}// 검색어만 있는 경우
				else if (productDTO.getProduct_searchKeyword() != null && !productDTO.getProduct_searchKeyword().isEmpty()) {
					System.out.println("model.ProductDAO.selectOne 키워드 검색 페이지 수 반환 쿼리 읽기 시작");
					args = new Object[]{ productDTO.getProduct_searchKeyword() };
					data = jdbcTemplate.queryForObject(PRODUCT_TOTAL_PAGE + " " + PRODUCT_SEARCH_PAGE, args, new ProductRowMapper_one_page_count());
				}//카테고리, 유형 있는경우 
				else if ((productDTO.getProduct_location() != null && !productDTO.getProduct_location().isEmpty()) ||
						(productDTO.getProduct_category() != null && !productDTO.getProduct_category().isEmpty())) {
					System.out.println("model.ProductDAO.selectOne 필터링 검색 페이지 수 반환 쿼리 읽기 시작");
					args = new Object[]{ productDTO.getProduct_location(), productDTO.getProduct_category() };
					data = jdbcTemplate.queryForObject(PRODUCT_TOTAL_PAGE + " " + PRODUCT_FILTERING_PAGE, args, new ProductRowMapper_one_page_count());
				}
				else {
					System.out.println("model.ProductDAO.selectOne 전체 페이지 수 반환 쿼리 읽기 시작");
					data = jdbcTemplate.queryForObject(PRODUCT_TOTAL_PAGE , new ProductRowMapper_one_page_count());
				}
			} 
			else {
				System.err.println("model.ProductDAO.selectOne 쿼리문 읽어오기 컨디션 실패");
			}
		}catch(Exception e){
			System.err.println("model.ProductDAO.selectOne 여기 실패!!!!!!! 널반환");
			return null;
		}

		System.out.println("model.ProductDAO.selectOne data : " + data);
		return data;
	}

	public boolean update(ProductDTO productDTO) { // 사장님 상품 변경
		System.out.println("====model.ProductDAO2.update 시작");

		// 변수 초기화
		String product_name = productDTO.getProduct_name();
		int product_price = productDTO.getProduct_price();
		String product_details = productDTO.getProduct_details();
		String product_address = productDTO.getProduct_address();
		String product_location = productDTO.getProduct_location();
		String product_category = productDTO.getProduct_category();
		String product_seller_id = productDTO.getProduct_seller_id();

		Object[] args = null;
		args = new Object[] { product_name, // 상품명
				product_price, // 가격
				product_details, // 설명
				product_address, // 주소
				product_location, // 위치
				product_category, // 카테고리
				product_seller_id // 판매자
		};
		int result = jdbcTemplate.update(PRODUCT_UPDATE, args);
		System.out.println("====model.ProductDAO2.update result : " + result);
		if (result <= 0) {
			System.err.println("====model.ProductDAO2.update 실패");
			return false;
		}
		System.out.println("====model.ProductDAO2.update 종료");
		return true; // 성공적으로 변경 완료
	}

	public boolean delete(ProductDTO productDTO) { // 입력
		System.out.println("====model.ProductDAO2.delete 시작");

		String product_seller_id = productDTO.getProduct_seller_id();
		int product_price = productDTO.getProduct_price();

		return false;
	}

	class ProductRowMapper_all implements RowMapper<ProductDTO> {

		@Override
		public ProductDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			System.out.println("model.ProductDAO.ProductRowMapper_all 실행");

			ProductDTO data = new ProductDTO();
			data.setProduct_num(rs.getInt("PRODUCT_NUM")); // 상품 번호
			data.setProduct_name(rs.getString("PRODUCT_NAME")); // 상품명
			data.setProduct_price(rs.getInt("PRODUCT_PRICE")); // 상품 가격
			data.setProduct_address(rs.getString("PRODUCT_ADDRESS")); // 상품 주소
			data.setProduct_location(rs.getString("PRODUCT_LOCATION")); // 상품 장소 (바다,민물)
			data.setProduct_category(rs.getString("PRODUCT_CATEGORY")); // 상품 유형 (낚시배, 낚시터,낚시카페, 수상)
			data.setProduct_avg_rating(rs.getDouble("RATING")); // 별점 평균
			data.setProduct_payment_cnt(rs.getInt("PAYMENT_COUNT")); // 결제 수
			data.setProduct_wishlist_cnt(rs.getInt("WISHLIST_COUNT")); // 찜 수
			data.setProduct_file_dir(rs.getString("FILE_DIR")); // 파일 경로

			System.out.println("model.ProductDAO.ProductRowMapper_all 반환");

			return data;
		}
	}
	class ProductRowMapper_filter_all implements RowMapper<ProductDTO> {

		@Override
		public ProductDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			System.out.println("model.ProductDAO.ProductRowMapper_all 실행");

			ProductDTO data = new ProductDTO();
			data.setProduct_num(rs.getInt("PRODUCT_NUM")); // 상품 번호
			data.setProduct_name(rs.getString("PRODUCT_NAME")); // 상품명
			data.setProduct_price(rs.getInt("PRODUCT_PRICE")); // 상품 가격
			data.setProduct_address(rs.getString("PRODUCT_ADDRESS")); // 상품 주소
			data.setProduct_location(rs.getString("PRODUCT_LOCATION")); // 상품 장소 (바다,민물)
			data.setProduct_category(rs.getString("PRODUCT_CATEGORY")); // 상품 유형 (낚시배, 낚시터,낚시카페, 수상)
			data.setProduct_file_dir(rs.getString("FILE_DIR")); // 파일 경로

			System.out.println("model.ProductDAO.ProductRowMapper_all 반환");

			return data;
		}
	}
	class ProductRowMapper_all_crawling implements RowMapper<ProductDTO> {

		@Override
		public ProductDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			System.out.println("model.ProductDAO.ProductRowMapper_all_crawling 실행");

			ProductDTO data = new ProductDTO();
			data.setProduct_num(rs.getInt("PRODUCT_NUM")); // 상품 번호

			System.out.println("model.ProductDAO.ProductRowMapper_all_crawling 반환");

			return data;
		}
	}

	//--------------------------------------------------one--------------------
	class ProductRowMapper_one_by_info implements RowMapper<ProductDTO> {
		// 상품 상세보기
		@Override
		public ProductDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			System.out.println("model.ProductDAO.ProductRowMapper_one_by_info 실행");

			ProductDTO data = new ProductDTO();
			data.setProduct_num(rs.getInt("PRODUCT_NUM")); // 상품 번호
			data.setProduct_name(rs.getString("PRODUCT_NAME")); // 상품명
			data.setProduct_price(rs.getInt("PRODUCT_PRICE")); // 상품 가격
			data.setProduct_details(rs.getString("PRODUCT_DETAILS")); // 상품 설명
			data.setProduct_address(rs.getString("PRODUCT_ADDRESS")); // 상품 주소
			data.setProduct_location(rs.getString("PRODUCT_LOCATION")); // 상품 장소 (바다,민물)
			data.setProduct_category(rs.getString("PRODUCT_CATEGORY")); // 상품 유형 (낚시배, 낚시터)
			data.setProduct_avg_rating(rs.getDouble("RATING")); // 별점 평균
			data.setProduct_payment_cnt(rs.getInt("PAYMENT_COUNT")); // 결제 수
			data.setProduct_wishlist_cnt(rs.getInt("WISHLIST_COUNT")); // 찜 수

			System.out.println("model.ProductDAO.ProductRowMapper_one_by_info 반환");

			return data;
		}
	}
	class ProductRowMapper_one_current_stock implements RowMapper<ProductDTO> {
		// 사용자가 선택한 일자의 재고 보기
		@Override
		public ProductDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			System.out.println("model.ProductDAO.ProductRowMapper_one_current_stock 실행");

			ProductDTO data = new ProductDTO();
			data.setProduct_num(rs.getInt("PRODUCT_NUM")); // 상품 번호
			data.setProduct_cnt(rs.getInt("CURRENT_STOCK")); // 상품의 재고

			System.out.println("model.ProductDAO.ProductRowMapper_one_current_stock 반환");

			return data;
		}
	}
	class ProductRowMapper_one_num implements RowMapper<ProductDTO> {
		// 상품번호 보여주기 (크롤링)
		@Override
		public ProductDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			System.out.println("model.ProductDAO.ProductRowMapper_one_num 실행");

			ProductDTO data = new ProductDTO();
			data.setProduct_num(rs.getInt("MAX_NUM")); // 상품 번호

			System.out.println("model.ProductDAO.ProductRowMapper_one_num 반환");

			return data;
		}
	}
	//	class ProductRowMapper_one_select implements RowMapper<ProductDTO> {
	//		// 전체 출력
	//		@Override
	//		public ProductDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
	//			System.out.println("model.ProductDAO.ProductRowMapper_one_select 실행");
	//
	//			ProductDTO data = new ProductDTO();
	//			data.setProduct_num(rs.getInt("PRODUCT_NUM")); // 상품 번호
	//
	//			System.out.println("model.ProductDAO.ProductRowMapper_one_select 반환");
	//
	//			return data;
	//		}
	//	}
	class ProductRowMapper_one_page_count implements RowMapper<ProductDTO> {
		// 상품 테이블의 전체 개수 출력
		@Override
		public ProductDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			System.out.println("model.ProductDAO.ProductRowMapper_one_page_count 실행");

			ProductDTO data = new ProductDTO();
			data.setProduct_total_page(rs.getInt("PRODUCT_TOTAL_PAGE")); // 상품 개수

			System.out.println("model.ProductDAO.ProductRowMapper_one_page_count 반환");

			return data;
		}
	}

}
