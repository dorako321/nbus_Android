package jp.nbus;

/**
 * お気に入りを構造体っぽく使うためのクラス(modified) Preferencesから取得したお気に入りの経路を入れる
 *
 * @author TeppeiIsayama
 *
 */
public class FavoriteRoutesAshDto {
	public int id;// DB上のid
	public int fm_id;
	public String fm_name;
	public String fm_ruby;
	public int to_id;
	public String to_name;
	public String to_ruby;
	public int co;
	public String co_name;
	public boolean isSearchFavorite; // idを含まない停留所名のみのお気に入りかどうか

	public FavoriteRoutesAshDto() {
	}

	/**
	 * FavoriteRoutes
	 *
	 * @param fm_id
	 *            乗車停留所id
	 * @param fm_name
	 *            乗車停留所名
	 * @param fm_ruby
	 *            乗車停留所名のふりがな
	 * @param to_id
	 *            降車停留所id
	 * @param to_name
	 *            降車停留所名
	 * @param to_ruby
	 *            降車停留所名のふりがな
	 * @param co
	 *            社局id
	 * @param co_name
	 *            社局名
	 */
	public FavoriteRoutesAshDto(int id, int fm_id, String fm_name, String fm_ruby,
			int to_id, String to_name, String to_ruby, int co, String co_name) {
		this.id = id;
		this.fm_id = fm_id;
		this.fm_name = fm_name;
		this.fm_ruby = fm_ruby;
		this.to_id = to_id;
		this.to_name = to_name;
		this.to_ruby = to_ruby;
		this.co = co;
		this.co_name = co_name;
		isSearchFavorite = false;
	}

	public FavoriteRoutesAshDto(int id, String fm_name, String to_name) {
		this.id = id;
		this.fm_name = fm_name;
		this.to_name = to_name;
		isSearchFavorite = true;
	}
}
